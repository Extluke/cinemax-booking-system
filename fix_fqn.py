import os
import re

BASE_DIR = r"d:\Kuliah\Tugas Kuliah Semester 5\IPPL\Tugas Besar\Program Files\Cinemax\src\main\java\com\cinemax\cinemax"

def remove_fqn():
    for root, _, filenames in os.walk(BASE_DIR):
        for fname in filenames:
            if fname.endswith(".java"):
                path = os.path.join(root, fname)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Remove fully qualified names in code but NOT in import statements or package statements
                
                # First, temporarily mask imports and packages
                imports = re.findall(r'^import\s+[^;]+;', content, re.MULTILINE)
                pkg_match = re.search(r'^package\s+[^;]+;', content, re.MULTILINE)
                
                # Now replace com.cinemax.cinemax.core. etc with empty string
                content = re.sub(r'com\.cinemax\.cinemax\.core\.', '', content)
                content = re.sub(r'com\.cinemax\.cinemax\.user\.', '', content)
                content = re.sub(r'com\.cinemax\.cinemax\.controller\.', '', content)
                content = re.sub(r'com\.cinemax\.cinemax\.dto\.', '', content)
                
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)

if __name__ == "__main__":
    remove_fqn()
    print("FQN removed.")
