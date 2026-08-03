# AtlasKV

A thread-safe, in-memory key-value store built in Java with Spring Boot. It supports GET/PUT/DELETE over REST, evicts entries with an LRU policy when it hits capacity, and survives a process crash by replaying a write-ahead log on startup.

This is a single-node project. It does not do replication, sharding, or any of the things a production system like Redis handles. The goal was to get the concurrency and durability guarantees of a small storage engine actually right, not to build something that competes with Redis.

## What it does

- O(1) GET, PUT, DELETE backed by a hash map and a doubly linked list
- LRU eviction when the store exceeds its configured capacity
- A single `ReentrantReadWriteLock` protecting all mutable state
- Write-ahead logging (append-only log) plus periodic snapshotting for persistence
- Recovery on startup: load the last snapshot, then replay the log on top of it
- Rollback of in-memory state if a write fails to persist, for inserts, updates, deletes, and eviction-triggered inserts

## API

| Method | Path | Description |
|---|---|---|
| `PUT` | `/store` | Store a key-value pair (JSON body: `{"key": ..., "value": ...}`) |
| `GET` | `/store/{key}` | Retrieve a value |
| `DELETE` | `/store/{key}` | Remove a key |
| `POST` | `/debug/crash-next-write` | Test hook: forces the next log write to fail partway through, to exercise recovery from a torn write |

## Why GET takes a write lock

The lock is a single `ReentrantReadWriteLock` at the service layer. GET uses the write side of it, not the read side, and that's worth explaining because it looks wrong at first glance.

Every GET moves the accessed node to the front of the LRU list to record that it was just used. That's a mutation of shared state, not a read, even though the value returned to the caller doesn't change. Letting multiple threads run GET concurrently under a read lock would mean multiple threads restructuring the same linked list at once, which corrupts it. So GET takes the same write lock as PUT and DELETE.

The consequence is that this store doesn't get concurrent reads. Every operation, including a plain lookup, is serialized against every other operation. That's a real cost, and the trade was made deliberately: a single lock across a small number of operations is something you can reason about with confidence, and reasoning about it correctly mattered more here than squeezing out extra throughput.

## Persistence and recovery

Writes are logged to an append-only file (`appendonly.aof` by default) as they happen, and the store also produces periodic snapshots (`snapshot.dat`) so the log doesn't grow without bound. On startup, `RecoveryManager` loads the most recent snapshot, then replays whatever log entries came after it.

Durability here is `flush()`, not `fsync()`. A `flush()` gets the data to the operating system, which is enough to survive the Java process dying. It is not enough to survive the OS itself crashing or a power loss before the OS gets around to writing that buffer to disk. If a write returns successfully from this store, it will survive a process crash. It will not necessarily survive the machine losing power a moment later. That's a real limitation, stated plainly rather than glossed over, since anyone using something like this should know exactly what guarantee they're getting.

If the log ends mid-write (the `crash-next-write` debug endpoint exists specifically to test this), the replayer discards the incomplete final entry rather than trying to parse it. Recovery reconstructs the same key-value contents that existed before shutdown. LRU recency order is not part of what's persisted, so after a restart the store still has the right data, just not the same "most recently used" ordering it had before the crash.

## Rollback

Every write does two things: it changes the in-memory structures (the map, the LRU list, and possibly an eviction), then it appends to the log. If the append fails, the in-memory change is rolled back rather than left in place. Without this, a failed write could leave the log and the in-memory state disagreeing with each other, meaning the client would be told an operation failed while the cache quietly kept the change anyway.

The one case that's not trivial to roll back is an insert that triggers an eviction, since that's really two structural changes at once: something is removed from the tail of the LRU list, and something new is added at the front. Rolling that back means putting the evicted node back in its exact former position, not just anywhere. `StorageEngine` captures the position of the evicted node before removing it, and rollback restores it there if the persistence write that followed fails. This is covered directly by `StorageServiceRollbackTest`, alongside separate tests for rolling back a plain update (`StorageServiceUpdateRollbackTest`) and a delete (`StorageServiceDeleteRollbackTest`).

## Invariants

These are checked directly by `StorageInvariantTest` and exercised under concurrent load by `StorageServiceStressTest` and `ConcurrentBenchmarkTest`:

- Every key in the map appears exactly once in the LRU list, and vice versa
- The cache never exceeds its configured capacity after a completed write
- The LRU list never contains a cycle
- Replaying the log after a restart reconstructs the same key-value contents that existed before shutdown
- A partially written log entry is discarded during recovery rather than corrupting state

## Running it

```
./gradlew bootRun
```

The server starts on port 8080. Storage capacity, and the paths for the log and snapshot files, are set in `src/main/resources/application.properties`.

## Testing

```
./gradlew test
```

Tests are organized by what they're checking rather than by class:

- `StorageEngineTest`, `LRUCacheTest` – core data structure correctness
- `StorageInvariantTest` – the invariants above, checked directly
- `StorageServiceConcurrencyTest`, `StorageServiceStressTest` – concurrent access from multiple threads
- `StorageServiceRollbackTest`, `StorageServiceUpdateRollbackTest`, `StorageServiceDeleteRollbackTest` – rollback on simulated persistence failure, one test per operation type
- `LogReplayTest` – recovery, including the partial-write case
- `ConcurrentBenchmarkTest` – a throughput and correctness check under concurrent load; the numbers it prints are from a short run on whatever machine it's run on, not a benchmark result to take as representative

## Out of scope

TTL expiration, replication, sharding, distributed operation, transactions, authentication, and metrics were all left out on purpose, to keep the scope to what a single process's concurrency and durability story actually requires. TTL in particular would need its own reasoning about how expiration interacts with eviction and the log.
