import os
import re

BASE_DIR = r"d:\Kuliah\Tugas Kuliah Semester 5\IPPL\Tugas Besar\Program Files\Cinemax\src\main\java\com\cinemax\cinemax"

class_to_package = {
    "BioskopConfig": "domain.config",
    "RefundPolicy": "domain.config",
    "AudioType": "domain.movie",
    "Film": "domain.movie",
    "FilmRepository": "domain.movie",
    "Genre": "domain.movie",
    "TipeStudio": "domain.movie",
    "TipeStudioRepository": "domain.movie",
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
    "Promo": "domain.booking",
    "PromoRepository": "domain.booking",
    "Tiket": "domain.booking",
    "TiketRepository": "domain.booking",
    "Transaksi": "domain.booking",
    "TransaksiRepository": "domain.booking",
    "User": "domain.user",
    "UserRepository": "domain.user",
    "Role": "domain.user",
    "UserRole": "domain.user",
    "PasswordResetToken": "domain.user",
    "PasswordResetTokenRepository": "domain.user",
    "RoleRepository": "domain.user",
    "MailService": "infrastructure.mail",
    "SecurityConfig": "infrastructure.security",
    "CustomUserDetailsService": "infrastructure.security",
    "WebConfig": "infrastructure.config",
    "DataInitializer": "infrastructure.config",
    "ApiJadwalController": "api.controller",
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

def fix_imports():
    for root, _, filenames in os.walk(BASE_DIR):
        for fname in filenames:
            if fname.endswith(".java"):
                path = os.path.join(root, fname)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                pkg_match = re.search(r'^package\s+([^;]+);', content, re.MULTILINE)
                current_pkg = pkg_match.group(1).strip() if pkg_match else ""
                
                # Clean up old bad imports
                content = re.sub(r'import\s+com\.cinemax\.cinemax\.core\..*;\n', '', content)
                content = re.sub(r'import\s+com\.cinemax\.cinemax\.user\..*;\n', '', content)
                content = re.sub(r'import\s+com\.cinemax\.cinemax\.domain\..*;\n', '', content) # Strip to rebuild

                imports_to_add = set()
                
                # Check for usage of any known class
                for cls, pkg in class_to_package.items():
                    full_pkg = f"com.cinemax.cinemax.{pkg}"
                    if full_pkg == current_pkg:
                        continue # Same package, no import needed
                        
                    # Check if class name is used as a word boundary
                    if re.search(r'\b' + cls + r'\b', content):
                        imports_to_add.add(f"import {full_pkg}.{cls};")

                if imports_to_add:
                    import_str = "\n".join(sorted(list(imports_to_add))) + "\n"
                    # Insert after package declaration
                    content = re.sub(
                        r'^(package\s+[^;]+;\n)',
                        r'\1\n' + import_str + '\n',
                        content,
                        count=1,
                        flags=re.MULTILINE
                    )

                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)

if __name__ == "__main__":
    fix_imports()
    print("Imports fixed.")
