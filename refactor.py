import os
import shutil
import re

BASE_DIR = r"d:\Kuliah\Tugas Kuliah Semester 5\IPPL\Tugas Besar\Program Files\Cinemax\src\main\java\com\cinemax\cinemax"

# Mapping Class -> Target Sub-Package
class_to_package = {
    # DOMAIN: Config
    "BioskopConfig": "domain.config",
    "RefundPolicy": "domain.config",

    # DOMAIN: Movie
    "AudioType": "domain.movie",
    "Film": "domain.movie",
    "FilmRepository": "domain.movie",
    "Genre": "domain.movie",
    "TipeStudio": "domain.movie",
    "TipeStudioRepository": "domain.movie",

    # DOMAIN: Schedule
    "Jadwal": "domain.schedule",
    "JadwalRepository": "domain.schedule",
    "Kursi": "domain.schedule",
    "KursiRepository": "domain.schedule",
    "Studio": "domain.schedule",
    "StudioRepository": "domain.schedule",
    "Fasilitas": "domain.schedule",
    "FasilitasRepository": "domain.schedule",
    "KelasKursi": "domain.schedule",
    "KelasKursiRepository": "domain.schedule",

    # DOMAIN: Booking
    "Promo": "domain.booking",
    "PromoRepository": "domain.booking",
    "Tiket": "domain.booking",
    "TiketRepository": "domain.booking",
    "Transaksi": "domain.booking",
    "TransaksiRepository": "domain.booking",

    # DOMAIN: User
    "User": "domain.user",
    "UserRepository": "domain.user",
    "Role": "domain.user",
    "UserRole": "domain.user",
    "PasswordResetToken": "domain.user",
    "PasswordResetTokenRepository": "domain.user",
    "RoleRepository": "domain.user",

    # INFRASTRUCTURE
    "MailService": "infrastructure.mail",
    "SecurityConfig": "infrastructure.security",
    "CustomUserDetailsService": "infrastructure.security",
    "WebConfig": "infrastructure.config",
    "DataInitializer": "infrastructure.config",

    # API
    "ApiJadwalController": "api.controller",

    # ADMIN
    "AdminPageController": "admin.controller",
    "AuthController": "admin.controller",
    "ErrorController": "admin.controller",
    "FileUploadController": "admin.controller",
    "GlobalControllerAdvice": "admin.controller",
    "ImageController": "admin.controller",
    "LandingPageController": "admin.controller",
    "TestingMailController": "admin.controller",
    "TiketUserController": "admin.controller",
    "JadwalRequest": "admin.dto"
}

def get_target_package(class_name, old_package):
    if class_name in class_to_package:
        return f"com.cinemax.cinemax.{class_to_package[class_name]}"
    
    # Fallbacks if not explicitly mapped
    if "controller" in old_package:
        return "com.cinemax.cinemax.admin.controller"
    if "dto" in old_package:
        return "com.cinemax.cinemax.admin.dto"
    if "security" in old_package:
        return "com.cinemax.cinemax.infrastructure.security"
    if "user" in old_package:
        return "com.cinemax.cinemax.domain.user"
    if "core" in old_package:
        return "com.cinemax.cinemax.domain.core"
    
    return old_package

def find_java_files():
    files = []
    for root, _, filenames in os.walk(BASE_DIR):
        for fname in filenames:
            if fname.endswith(".java"):
                files.append(os.path.join(root, fname))
    return files

def parse_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    pkg_match = re.search(r'^package\s+([^;]+);', content, re.MULTILINE)
    old_pkg = pkg_match.group(1).strip() if pkg_match else "com.cinemax.cinemax"
    class_name = os.path.basename(path).replace(".java", "")
    
    if class_name == "CinemaxApplication":
        new_pkg = old_pkg # Keep main class at root
    else:
        new_pkg = get_target_package(class_name, old_pkg)
        
    return {
        "path": path,
        "content": content,
        "class_name": class_name,
        "old_pkg": old_pkg,
        "new_pkg": new_pkg,
        "old_fqn": f"{old_pkg}.{class_name}",
        "new_fqn": f"{new_pkg}.{class_name}"
    }

def main():
    files = find_java_files()
    parsed_files = [parse_file(f) for f in files]
    
    # Build replacement rules
    replacements = []
    for info in parsed_files:
        if info["old_pkg"] != info["new_pkg"]:
            replacements.append((info["old_fqn"], info["new_fqn"]))
            
    # Also add wildcards just in case
    replacements.append(("com.cinemax.cinemax.core.*", "com.cinemax.cinemax.domain.*"))

    for info in parsed_files:
        content = info["content"]
        
        # 1. Update its own package declaration
        if info["old_pkg"] != info["new_pkg"]:
            content = re.sub(
                r'^package\s+([^;]+);', 
                f'package {info["new_pkg"]};', 
                content, 
                flags=re.MULTILINE
            )
            
        # 2. Update imports
        for old_fqn, new_fqn in replacements:
            if old_fqn != new_fqn:
                content = content.replace(f"import {old_fqn};", f"import {new_fqn};")
                
        # Fix wildcards manually
        if "import com.cinemax.cinemax.core.*;" in info["content"]:
             imports_to_add = "\nimport com.cinemax.cinemax.domain.movie.*;\nimport com.cinemax.cinemax.domain.schedule.*;\nimport com.cinemax.cinemax.domain.booking.*;\nimport com.cinemax.cinemax.domain.config.*;\nimport com.cinemax.cinemax.domain.user.*;\n"
             content = content.replace("import com.cinemax.cinemax.core.*;", imports_to_add)

        if "import com.cinemax.cinemax.user.*;" in info["content"]:
             imports_to_add = "\nimport com.cinemax.cinemax.domain.user.*;\nimport com.cinemax.cinemax.infrastructure.mail.*;\n"
             content = content.replace("import com.cinemax.cinemax.user.*;", imports_to_add)

        # 3. Write to new file location
        if info["class_name"] != "CinemaxApplication":
            rel_path = info["new_pkg"].replace("com.cinemax.cinemax.", "").replace(".", "\\")
            new_dir = os.path.join(BASE_DIR, rel_path)
        else:
            new_dir = BASE_DIR
            
        os.makedirs(new_dir, exist_ok=True)
        new_path = os.path.join(new_dir, f'{info["class_name"]}.java')
        
        with open(new_path, 'w', encoding='utf-8') as f:
            f.write(content)
            
        # Remove old file if it moved
        if new_path != info["path"] and os.path.exists(info["path"]):
            os.remove(info["path"])

    # Clean up empty directories
    for root, dirs, files in os.walk(BASE_DIR, topdown=False):
        for d in dirs:
            dir_path = os.path.join(root, d)
            if not os.listdir(dir_path):
                os.rmdir(dir_path)

    print("Refactoring complete.")

if __name__ == "__main__":
    main()
