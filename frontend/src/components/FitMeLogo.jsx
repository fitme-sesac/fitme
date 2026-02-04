/**
 * FITME 로고 - 왼쪽 도형(타겟 아이콘) + FITME 텍스트
 */
export default function FitMeLogo({ height = 50, className = "", style = {} }) {
    return (
        <span
            className={`d-flex align-items-center gap-2 ${className}`}
            style={{ ...style }}
        >
            <img
                src="/assets/img/main/fitme_logo_icon.png"
                alt=""
                aria-hidden
                style={{
                    height: typeof height === "number" ? `${height}px` : height,
                    width: "auto",
                    display: "block",
                }}
            />
            <span
                className="fw-bold text-dark"
                style={{
                    fontSize: height ? (typeof height === "number" ? height * 0.56 : "1.4rem") : "1.4rem",
                    letterSpacing: "0.02em",
                }}
            >
                FITME
            </span>
        </span>
    );
}
