package com.atlaskv.controller;

import org.springframework.web.bind.annotation.PostMapping;

import com.atlaskv.persistence.AppendOnlyLog;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final AppendOnlyLog appendOnlyLog;

    public DebugController(AppendOnlyLog appendOnlyLog) {
        this.appendOnlyLog = appendOnlyLog;
    }

    @PostMapping("/crash-next-write")
    public String crashNextWrite() {
        appendOnlyLog.enableCrashAfterPartialWrite();
        return "Crash enabled";
    }
}