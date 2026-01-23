import sys
import io
import requests
import json

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

def test_summary_api(resume_id):
    url = f"http://127.0.0.1:8000/resumes/{resume_id}/summary"
    print(f"Sending POST request to {url}...")
    
    try:
        response = requests.post(url)
        if response.status_code == 200:
            print("[SUCCESS] API Call Successful!")
            result = response.json()
            print(json.dumps(result, indent=2, ensure_ascii=False))
        else:
            print(f"[FAILED] Failed with status code {response.status_code}")
            print(response.text)
    except Exception as e:
        print(f"[ERROR] Error: {e}")

if __name__ == "__main__":
    resume_id = 5002
    if len(sys.argv) > 1:
        resume_id = int(sys.argv[1])
    test_summary_api(resume_id)
