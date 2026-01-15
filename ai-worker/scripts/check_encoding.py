
import chardet

with open("d:\\fitme_pj\\ai-worker\\.env", 'rb') as f:
    result = chardet.detect(f.read())
    print(result)
