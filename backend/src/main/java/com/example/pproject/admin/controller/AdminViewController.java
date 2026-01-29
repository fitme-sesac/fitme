package com.example.pproject.admin.controller;

import com.example.pproject.admin.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // @RestController가 아님! (HTML을 반환)
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final AdminMemberService adminMemberService;

    // 대시보드 메인 화면
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 1. 서비스에서 통계 데이터 가져오기 (아까 만든 그 메서드!)
        var stats = adminMemberService.getMemberManagementStats();

        // 2. HTML에 데이터 심기 (이름, 값)
        model.addAttribute("stats", stats);

        // 3. dashboard.html 파일을 보여줘라
        return "admin/dashboard";
    }
}