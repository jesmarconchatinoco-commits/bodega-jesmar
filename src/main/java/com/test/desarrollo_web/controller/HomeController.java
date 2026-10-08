package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.service.DashboardService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Controller
public class HomeController {

    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public HomeController(DashboardService dashboardService, ObjectMapper objectMapper) {
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) throws Exception {
        Map<String, Object> resumen = dashboardService.obtenerResumen();
        model.addAllAttributes(resumen);
        model.addAttribute("dashboardChartJson", objectMapper.writeValueAsString(Map.of(
                "ventasUltimos7Dias", resumen.get("ventasUltimos7Dias"),
                "ventasPorCanal", resumen.get("ventasPorCanal"),
                "gananciasVentas", resumen.get("gananciasVentas")
        )));
        return "dashboard";
    }

    @GetMapping(value = "/dashboard/api/ganancias", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> gananciasApi(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "DIA") String periodo) {
        return dashboardService.obtenerGanancias(periodo);
    }

    @GetMapping(value = "/dashboard/api/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> statsApi() {
        return dashboardService.obtenerResumen();
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
