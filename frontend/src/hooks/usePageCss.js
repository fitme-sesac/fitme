import { useEffect } from "react";

/**
 * 기존 static/assets/css/pages/*.css 를 페이지 단위로 붙이는 용도.
 * 예) usePageCss("/assets/css/pages/login.css");
 */
export function usePageCss(href) {
    useEffect(() => {
        if (!href) return;

        const id = `page-css:${href}`;
        let link = document.getElementById(id);

        if (!link) {
            link = document.createElement("link");
            link.id = id;
            link.rel = "stylesheet";
            link.href = href;
            document.head.appendChild(link);
        }

        // ✅ 페이지 이동 시 제거해서 다른 페이지에 CSS가 남지 않게 함
        return () => {
            link?.remove();
        };
    }, [href]);
}
