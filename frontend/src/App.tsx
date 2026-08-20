import { Navigate, Route, Routes } from "react-router-dom";
import { GuestRoute, ProtectedRoute, RoleRoute } from "./components/ProtectedRoute";
import { CustomerDashboard } from "./customer/CustomerDashboard";
import { AdminApplicationPage } from "./pages/AdminApplicationPage";
import { AdminHome } from "./pages/AdminHome";
import { GoogleCallbackPage } from "./pages/GoogleCallbackPage";
import { LandingPage } from "./pages/LandingPage";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { VerifyOtpPage } from "./pages/VerifyOtpPage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route element={<GuestRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>
      <Route path="/auth/google/callback" element={<GoogleCallbackPage />} />
      <Route path="/verify-otp" element={<VerifyOtpPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<RoleRoute role="CUSTOMER" />}>
          <Route path="/customer" element={<CustomerDashboard />} />
          <Route path="/verify" element={<Navigate to="/customer" replace />} />
          <Route path="/dashboard" element={<Navigate to="/customer" replace />} />
        </Route>
        <Route element={<RoleRoute role="ADMIN" />}>
          <Route path="/admin" element={<AdminHome />} />
          <Route path="/admin/applications" element={<AdminHome />} />
          <Route path="/admin/users" element={<AdminHome />} />
          <Route path="/admin/knowledge" element={<AdminHome />} />
          <Route path="/admin/settings" element={<AdminHome />} />
          <Route path="/admin/reports" element={<Navigate to="/admin" replace />} />
          <Route path="/admin/applications/:userId" element={<AdminApplicationPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
