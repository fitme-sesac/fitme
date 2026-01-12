import { Outlet, Link } from "react-router-dom";

export default function Layout() {
  return (
    <div>
      {/* 기존 fragments/header.html을 React 컴포넌트로 옮기면 여기 교체 */}
      <header style={{ padding: 16, borderBottom: "1px solid #eee" }}>
        <nav style={{ display: "flex", gap: 12 }}>
          <Link to="/login">Login</Link>
          <Link to="/register">Register</Link>
          <Link to="/find-userid">Find UserID</Link>
          <Link to="/reset-password">Reset Password</Link>
        </nav>
      </header>

      <main style={{ padding: 16 }}>
        <Outlet />
      </main>

      {/* 기존 fragments/footer.html을 React 컴포넌트로 옮기면 여기 교체 */}
      <footer style={{ padding: 16, borderTop: "1px solid #eee" }}>
        <small>© PProject</small>
      </footer>
    </div>
  );
}
