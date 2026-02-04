import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { http } from "@/api/http";

export default function ResetPasswordPage() {
    const navigate = useNavigate();
    const [step, setStep] = useState(1);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState("");

    // step1
    const [req, setReq] = useState({ userid: "", username: "", birthday: "" });

    // step2
    const [inputCode, setInputCode] = useState("");

    // step3
    const [pw, setPw] = useState({ newPassword: "", confirmPassword: "" });

    const submitStep1 = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError("");
        
        try {
            const params = new URLSearchParams();
            params.append("userid", req.userid);
            params.append("username", req.username);
            params.append("birthday", req.birthday);
            
            const response = await http.post("/api/auth/password/reset/request", params);
            
            if (response.data.ok) {
                setStep(2);
            } else {
                setError(response.data.message || "요청 처리에 실패했습니다.");
            }
        } catch (err) {
            setError(err.response?.data?.message || "요청 처리에 실패했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    const submitStep2 = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError("");
        
        try {
            const params = new URLSearchParams();
            params.append("userid", req.userid);
            params.append("inputCode", inputCode);
            
            const response = await http.post("/api/auth/password/reset/verify", params);
            
            if (response.data.ok) {
                setStep(3);
            } else {
                setError(response.data.message || "인증에 실패했습니다.");
            }
        } catch (err) {
            setError(err.response?.data?.message || "인증에 실패했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    const submitStep3 = async (e) => {
        e.preventDefault();
        if (pw.newPassword !== pw.confirmPassword) {
            setError("비밀번호가 일치하지 않습니다.");
            return;
        }
        
        setIsLoading(true);
        setError("");
        
        try {
            const params = new URLSearchParams();
            params.append("userid", req.userid);
            params.append("newPassword", pw.newPassword);
            params.append("confirmPassword", pw.confirmPassword);
            
            const response = await http.post("/api/auth/password/reset/confirm", params);
            
            if (response.data.ok) {
                alert("비밀번호가 성공적으로 변경되었습니다.");
                navigate("/auth");
            } else {
                setError(response.data.message || "비밀번호 변경에 실패했습니다.");
            }
        } catch (err) {
            setError(err.response?.data?.message || "비밀번호 변경에 실패했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: 560 }}>
            <h2 className="mb-3">비밀번호 재설정</h2>

            <div className="mb-3">
                <span className={step === 1 ? "fw-bold" : ""}>1) 사용자 확인</span>{" "}
                → <span className={step === 2 ? "fw-bold" : ""}>2) 코드 인증</span>{" "}
                → <span className={step === 3 ? "fw-bold" : ""}>3) 새 비밀번호</span>
            </div>

            {error && (
                <div className="alert alert-danger" role="alert">
                    {error}
                </div>
            )}

            {step === 1 && (
                <form onSubmit={submitStep1} className="d-grid gap-3">
                    <div>
                        <label className="form-label" htmlFor="userid">아이디</label>
                        <input id="userid" className="form-control" value={req.userid}
                               onChange={(e) => setReq((p) => ({ ...p, userid: e.target.value }))}
                               required disabled={isLoading} />
                    </div>
                    <div>
                        <label className="form-label" htmlFor="username">이름</label>
                        <input id="username" className="form-control" value={req.username}
                               onChange={(e) => setReq((p) => ({ ...p, username: e.target.value }))}
                               required disabled={isLoading} />
                    </div>
                    <div>
                        <label className="form-label" htmlFor="birthday">생년월일(YYMMDD)</label>
                        <input id="birthday" className="form-control" value={req.birthday}
                               onChange={(e) => setReq((p) => ({ ...p, birthday: e.target.value }))}
                               required disabled={isLoading} placeholder="예: 901231" />
                    </div>
                    <button className="btn btn-primary" type="submit" disabled={isLoading}>
                        {isLoading ? "처리중..." : "인증코드 받기"}
                    </button>
                </form>
            )}

            {step === 2 && (
                <form onSubmit={submitStep2} className="d-grid gap-3">
                    <div className="text-muted">아이디: {req.userid}</div>
                    <div className="alert alert-info">
                        등록된 이메일로 인증코드가 발송되었습니다.
                    </div>
                    <div>
                        <label className="form-label" htmlFor="inputCode">인증코드 (6자리)</label>
                        <input id="inputCode" className="form-control" value={inputCode}
                               onChange={(e) => setInputCode(e.target.value)}
                               required disabled={isLoading} placeholder="6자리 인증코드 입력" />
                    </div>
                    <div className="d-flex gap-2">
                        <button className="btn btn-primary" type="submit" disabled={isLoading}>
                            {isLoading ? "확인중..." : "확인"}
                        </button>
                        <button className="btn btn-outline-secondary" type="button" onClick={() => { setStep(1); setError(""); }} disabled={isLoading}>
                            이전
                        </button>
                    </div>
                </form>
            )}

            {step === 3 && (
                <form onSubmit={submitStep3} className="d-grid gap-3">
                    <div className="text-muted">아이디: {req.userid}</div>
                    <div>
                        <label className="form-label" htmlFor="newPassword">새 비밀번호</label>
                        <input id="newPassword" type="password" className="form-control" value={pw.newPassword}
                               onChange={(e) => setPw((p) => ({ ...p, newPassword: e.target.value }))}
                               required disabled={isLoading} />
                    </div>
                    <div>
                        <label className="form-label" htmlFor="confirmPassword">새 비밀번호 확인</label>
                        <input id="confirmPassword" type="password" className="form-control" value={pw.confirmPassword}
                               onChange={(e) => setPw((p) => ({ ...p, confirmPassword: e.target.value }))}
                               required disabled={isLoading} />
                    </div>
                    <div className="d-flex gap-2">
                        <button className="btn btn-primary" type="submit" disabled={isLoading}>
                            {isLoading ? "변경중..." : "비밀번호 변경"}
                        </button>
                        <button className="btn btn-outline-secondary" type="button" onClick={() => { setStep(2); setError(""); }} disabled={isLoading}>
                            이전
                        </button>
                    </div>
                </form>
            )}
        </div>
    );
}
