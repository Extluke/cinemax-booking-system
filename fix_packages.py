import os
import re

BASE_DIR = r"d:\Kuliah\Tugas Kuliah Semester 5\IPPL\Tugas Besar\Program Files\Cinemax\src\main\java\com\cinemax\cinemax"

mapping = {
    "AuditLog.java": "domain.config",
    "AuditLogRepository.java": "domain.config",
    "AuditService.java": "domain.config",
    "BioskopConfigRepository.java": "domain.config",
    "GenreRepository.java": "domain.movie",
    "Promosi.java": "domain.booking",
    "PromosiRepository.java": "domain.booking",
    "Refund.java": "domain.booking",
    "RefundRepository.java": "domain.booking"
}

for fname, new_pkg in mapping.items():
    # search for it
    found = False
    for root, _, files in os.walk(BASE_DIR):
        if fname in files:
            path = os.path.join(root, fname)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            content = re.sub(r'^package\s+[^;]+;', f'package com.cinemax.cinemax.{new_pkg};', content, count=1, flags=re.MULTILINE)
            
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Updated {fname}")
            found = True
            break
