import os
import re

BASE_DIR = r"d:\Kuliah\Tugas Kuliah Semester 5\IPPL\Tugas Besar\Program Files\Cinemax\src\main\java\com\cinemax\cinemax"

def auto_fix_imports():
    # 1. Build a map of ClassName -> Full Package
    class_map = {}
    java_files = []
    
    for root, _, filenames in os.walk(BASE_DIR):
        for fname in filenames:
            if fname.endswith(".java"):
                path = os.path.join(root, fname)
                java_files.append(path)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                pkg_match = re.search(r'^package\s+([^;]+);', content, re.MULTILINE)
                if pkg_match:
                    pkg = pkg_match.group(1).strip()
                    class_name = fname.replace(".java", "")
                    class_map[class_name] = f"{pkg}.{class_name}"

    # 2. Add imports for any used class
    for path in java_files:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()

        pkg_match = re.search(r'^package\s+([^;]+);', content, re.MULTILINE)
        current_pkg = pkg_match.group(1).strip() if pkg_match else ""

        # Remove old unresolved imports
        content = re.sub(r'import\s+com\.cinemax\.cinemax\.core\..*;\n', '', content)
        content = re.sub(r'import\s+com\.cinemax\.cinemax\.user\..*;\n', '', content)
        content = re.sub(r'import\s+com\.cinemax\.cinemax\.controller\..*;\n', '', content)
        content = re.sub(r'import\s+com\.cinemax\.cinemax\.security\..*;\n', '', content)

        imports_to_add = set()
        
        # We also need to avoid adding imports for things in the SAME package or java.lang
        for cls_name, full_fqn in class_map.items():
            cls_pkg = full_fqn.rsplit('.', 1)[0]
            if cls_pkg == current_pkg:
                continue # Same package

            # If class name is used as a whole word in the file
            if re.search(r'\b' + cls_name + r'\b', content):
                imports_to_add.add(f"import {full_fqn};")

        # 3. Insert imports
        if imports_to_add:
            # We must be careful not to duplicate imports that might already be there (with new names)
            existing_imports = re.findall(r'^import\s+[^;]+;', content, re.MULTILINE)
            
            final_imports = set(imports_to_add)
            for imp in existing_imports:
                final_imports.add(imp)
                
            # Remove all existing imports to rewrite them neatly
            content = re.sub(r'^import\s+[^;]+;\n', '', content, flags=re.MULTILINE)
            
            import_str = "\n".join(sorted(list(final_imports))) + "\n\n"
            content = re.sub(
                r'^(package\s+[^;]+;\n)',
                r'\1\n' + import_str,
                content,
                count=1,
                flags=re.MULTILINE
            )
            
            # Clean up excessive newlines
            content = re.sub(r'\n{3,}', '\n\n', content)

        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)

if __name__ == "__main__":
    auto_fix_imports()
    print("Auto-import completed.")
