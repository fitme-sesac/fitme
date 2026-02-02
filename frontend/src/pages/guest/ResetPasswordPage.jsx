import { useState } from "react";

export default function ResetPasswordPage() {
    const [step, setStep] = useState(1);

    // step1
    const [req, setReq] = useState({ userid: "", username: "", birthday: "" });

    // step2
    const [inputCode, setInputCode] = useState("");

    // step3
    const [pw, setPw] = useState({ newPassword: "", confirmPassword: "" });

    const submitStep1 = (e) => {
        e.preventDefault();
        // TODO: /api/auth/password/reset/request
        console.log("step1", req);
        setStep(2);
    };

    const submitStep2 = (e) => {
        e.preventDefault();
        // TODO: /api/auth/password/reset/verify
        console.log("step2", { userid: req.userid, inputCode });
        setStep(3);
    };

    const submitStep3 = (e) => {
        e.preventDefault();
        if (pw.newPassword !== pw.confirmPassword) {
            alert("비밀번호가 일치하지 않습니다.");
            return;
        }
        // TODO: /api/auth/password/reset/confirm
        console.log("step3", { userid: req.userid, ...pw });
        alert("비밀번호 변경 요청은 다음 단계에서 API 연동합니다.");
    };

    return (
        <div style={{ maxWidth: 560 }}>
            <h2 className="mb-3">Reset Password</h2>

            <div className="mb-3">
                <span className={step === 1 ? "fw-bold" : ""}>1) 사용자 확인</span>{" "}
                → <span className={step === 2 ? "fw-bold" : ""}>2) 코드 인증</span>{" "}
                → <span className={step === 3 ? "fw-bold" : ""}>3) 새 비밀번호</span>
            </div>

            {step === 1 && (
                <form onSubmit={submitStep1} className="d-grid gap-3">
                    <div>
                        <label className="form-label" htmlFor="userid">아이디</label>
                        <input id="userid" className="form-control" value={req.userid}
                               onChange={(e) => setReq((p) => ({ ...p, userid: e.target.value }))}
                               required />
                    </div>
                    <div>
                        <label className="form-label" htmlFor="username">이름</label>
                        <input id="username" className="form-control" value={req.username}
                               onChange={(e) => setReq((p) => ({ ...p, username: e.target.value }))}
                               required />
                    </div>
                    <div>
                        <label className="form-label" htmlFor="birthday">생년월일(YYMMDD)</label>
                        <input id="birthday" className="form-control" value={req.birthday}
                               onChange={(e) => setReq((p) => ({ ...p, birthday: e.target.value }))}
                               required />
                    </div>
                    <button className="btn btn-primary" type="submit">인증코드 받기</button>
                </form>
            )}

            {step === 2 && (
                <form onSubmit={submitStep2} className="d-grid gap-3">
                    <div className="text-muted">아이디: {req.userid}</div>
                    <div>
                        <label className="form-label" htmlFor="inputCode">인증코드</label>
                        <input id="inputCode" className="form-control" value={inputCode}
                               onChange={(e) => setInputCode(e.target.value)}
                               required />
                    </div>
                    <div className="d-flex gap-2">
                        <button className="btn btn-primary" type="submit">확인</button>
                        <button className="btn btn-outline-secondary" type="button" onClick={() => setStep(1)}>
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
                               required />
                    </div>
                    <div>
                        <label className="form-label" htmlFor="confirmPassword">새 비밀번호 확인</label>
                        <input id="confirmPassword" type="password" className="form-control" value={pw.confirmPassword}
                               onChange={(e) => setPw((p) => ({ ...p, confirmPassword: e.target.value }))}
                               required />
                    </div>
                    <div className="d-flex gap-2">
                        <button className="btn btn-primary" type="submit">변경</button>
                        <button className="btn btn-outline-secondary" type="button" onClick={() => setStep(2)}>
                            이전
                        </button>
                    </div>
                </form>
            )}
        </div>
    );
}
