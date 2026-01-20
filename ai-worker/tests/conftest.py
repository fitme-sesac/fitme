# Pytest 설정 파일 (현재는 비어있지만 추후 DB fixture 등을 정의)
import sys
import os

# app 모듈을 찾을 수 있도록 경로 추가
# 파이썬은 폴더가 다르면 import app... 할 때 경로 에러가 자주 납니다. 이 파일에서 sys.path.append(...)를 해줘서, 다른 테스트 파일들이 app 폴더를 편하게 불러다 쓸 수 있게 "길을 뚫어주는 역할"을 합니다.
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
