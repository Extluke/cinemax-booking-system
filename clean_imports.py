import os
import re

BASE_DIR = r"d:\Kuliah\Tugas Kuliah Semester 5\IPPL\Tugas Besar\Program Files\Cinemax\src\main\java\com\cinemax\cinemax"

def fix_line_endings():
    for root, _, filenames in os.walk(BASE_DIR):
        for fname in filenames:
            if fname.endswith(".java"):
                path = os.path.join(root, fname)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Remove any leftover bad imports
                content = re.sub(r'import\s+com\.cinemax\.cinemax\.domain\.core\.[^;]+;\r?\n?', '', content)
                content = re.sub(r'import\s+com\.cinemax\.cinemax\.core\.[^;]+;\r?\n?', '', content)
                content = re.sub(r'import\s+com\.cinemax\.cinemax\.user\.[^;]+;\r?\n?', '', content)
                content = re.sub(r'import\s+com\.cinemax\.cinemax\.controller\.[^;]+;\r?\n?', '', content)
                
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)

if __name__ == "__main__":
    fix_line_endings()
    print("Done")
