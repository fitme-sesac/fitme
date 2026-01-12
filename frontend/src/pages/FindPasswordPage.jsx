import HtmlPage from "../components/HtmlPage";

import { usePageCss } from "../hooks/usePageCss";

const html = `<main class="main">

  <!-- Page Title -->
  <div class="page-title" style="background-color: #003300;">
    <div class="container text-center">
      <h1 style="color: white;">회원 서비스</h1>
      <nav class="breadcrumbs">
        <div>
          <span class="responsive-span" style="color: white;">한국 야구를 사랑하는, KIA팬 여러분의 열정 가득한 응원 공간에 오신 것을 환영합니다!</span>
        </div>
      </nav>
    </div>
  </div>

  <section id="services-2" class="services-2 section">
    <div class="container mb-3">
      <div class="row justify-content-center" data-aos="fade-up">
        <div class="col-md-6 col-lg-6">

          <h2 style="font-weight: bold; margin-bottom: 3rem;">비밀번호 찾기</h2>

          <div class="alert alert-danger" role="alert"></div>

          <form id="findPasswordForm" method="post">
            <div class="input-wrap">
              <label for="userid" style="margin-bottom: 1rem"><strong>아이디</strong></label>
              <input type="text" id="userid" name="userid" required placeholder="아이디를 입력하세요"
                     onfocus="this.placeholder = ''" onblur="this.placeholder = '아이디를 입력하세요'">
            </div>

            <div class="input-wrap">
              <label for="username" style="margin-bottom: 1rem"><strong>이름</strong></label>
              <input type="text" id="username" name="username" required placeholder="이름을 입력하세요"
                     onfocus="this.placeholder = ''" onblur="this.placeholder = '이름을 입력하세요'">
            </div>

            <div class="input-wrap">
              <label for="email" style="margin-bottom: 1rem"><strong>이메일</strong></label>
              <input type="text" id="email" name="email" required placeholder="이메일을 입력하세요"
                     onfocus="this.placeholder = ''" onblur="this.placeholder = '이메일을 입력하세요'">
            </div>

            <div style="display:flex; justify-content:flex-end; width:100%; margin-top:0.2rem">
              <button type="submit" class="btn btn-success rounded-pill">비밀번호 변경 링크 받기</button>
            </div>

            <ul class="link-wrap mb-3" style="margin-top: 0.6rem;">
              <li><a href="#" onclick="location.href='/User/Find_Userid'">아이디 찾기</a> |</li>
              <li><a href="#" onclick="location.href='/Login'">로그인</a> |</li>
              <li><a href="#" onclick="location.href='/User/Register'">회원가입</a></li>
            </ul>

            <div class="alert alert-info mt-3" style="display:none;"></div>
          </form>

        </div>
      </div>
    </div>
  </section>
</main>`;

const scripts = [
  "(() => {\n" +
    "  const form = document.getElementById('findPasswordForm');\n" +
    "  if (!form) return;\n" +
    "  const err = document.querySelector('.alert.alert-danger');\n" +
    "  const info = document.querySelector('.alert.alert-info');\n" +
    "  const setErr = (msg) => {\n" +
    "    if (!err) { alert(msg); return; }\n" +
    "    err.textContent = msg || '';\n" +
    "    err.style.display = msg ? 'block' : 'none';\n" +
    "  };\n" +
    "  const setInfo = (msg) => {\n" +
    "    if (!info) return;\n" +
    "    info.textContent = msg || '';\n" +
    "    info.style.display = msg ? 'block' : 'none';\n" +
    "  };\n" +
    "  setErr('');\n" +
    "  const params = new URLSearchParams(window.location.search);\n" +
    "  const em = params.get('errorMessage') || params.get('error') || '';\n" +
    "  if (em) setErr(em);\n" +
    "  setInfo('');\n" +
    "\n" +
    "  form.addEventListener('submit', async (e) => {\n" +
    "    e.preventDefault();\n" +
    "    setErr('');\n" +
    "    setInfo('');\n" +
    "\n" +
    "    const fd = new FormData(form);\n" +
    "    // 서버가 지원하면: /User/Find_Password_Link (링크 발송)\n" +
    "    // 미지원이면 안내 메시지 표시\n" +
    "    try {\n" +
    "      const res = await fetch('/User/Find_Password_Link', { method: 'POST', body: fd });\n" +
    "      if (res.redirected) { window.location.href = res.url; return; }\n" +
    "      if (!res.ok) {\n" +
    "        try {\n" +
    "          const data = await res.json();\n" +
    "          setErr((data && data.message) ? data.message : '요청을 처리할 수 없습니다.');\n" +
    "        } catch (e) {\n" +
    "          setErr('요청을 처리할 수 없습니다.');\n" +
    "        }\n" +
    "        return;\n" +
    "      }\n" +
    "      setInfo('입력하신 이메일로 비밀번호 변경 링크를 발송했습니다.');\n" +
    "    } catch (err) {\n" +
    "      setErr('서버 연결에 실패했습니다.');\n" +
    "    }\n" +
    "  });\n" +
    "})();",
  "AOS.init();",
];

export default function FindPasswordPage() {
  usePageCss("/assets/css/pages/find_password.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
