package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getGlobalStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> stats = statisticsService.getGlobalStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/formateurs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllFormateursStatistics() {
        List<Map<String, Object>> stats = statisticsService.getAllFormateursStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/formateurs/{formateurId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getFormateurStatistics(@PathVariable Long formateurId) {
        Map<String, Object> stats = statisticsService.getFormateurStatistics(formateurId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/sessions/{sessionId}/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSessionDetails(@PathVariable Long sessionId) {
        Map<String, Object> details = statisticsService.getSessionDetails(sessionId);
        return ResponseEntity.ok(details);
    }
}

