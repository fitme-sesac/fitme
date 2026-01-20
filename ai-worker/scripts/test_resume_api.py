import requests
import json
import sys
import io

# 터미널 한글 출력 설정
sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8')

def test_json_payload():
    API_URL = "http://localhost:8000/resumes/process"
    
    # Corrected JSON payload
    payload = {
        "resume_id": 1,
        "basic_info": {
            "title": "기본기를 바탕으로 확장성 있는 코드를 고민하는 신입 백엔드 개발자",
            "re_stack": [],
            "field": "RESUME",
            "preference": {
                "location": "서울 전체",
                "salary": "3,400만 원 이상",
                "employment_type": "FULL_TIME"
            }
        },
        "content": "",
        "file_links": [
            "D:\\fitme_pj\\ai-worker\\temp_pdfs\\포트폴리오_고상진.pdf", 
            "D:\\fitme_pj\\ai-worker\\temp_pdfs\\자기소개서_고상진.pdf"
        ],
        "projects": [],
        "careers": [],
        "summary_type": "STRUCTURED",
        "include_reasoning": True
    }

    print(f"📡 Sending request to {API_URL}...")
    try:
        response = requests.post(API_URL, json=payload)
        
        if response.status_code == 200:
            print("✅ Success! Response:")
            print(json.dumps(response.json(), indent=2, ensure_ascii=False))
        else:
            print(f"❌ Failed with status code: {response.status_code}")
            print(response.text)
            
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    test_json_payload()
