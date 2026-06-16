package com.emmanuelandsamuel.savings_project.services.interfaces;

public interface AdminService {
    String suspendUserWallet(String userEmail, String message);
    String suspendGroup(String groupCode, String message);
}
