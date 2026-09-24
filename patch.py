import re

with open('d:/Kuliah/Tugas Kuliah Semester 5/IPPL/Tugas Besar/Program Files/Cinemax/src/main/resources/templates/admin/pengaturan_admin.html', 'r', encoding='utf-8') as f: 
    c = f.read()

for day in ['senin', 'selasa', 'rabu', 'kamis', 'jumat', 'sabtu', 'minggu']:
    old_str = f'id="{day}-time-inputs" style="display: flex; gap: 8px; margin-left: 12px;"'
    new_str = f'id="{day}-time-inputs" class="time-inputs" th:style="${{config.{day}Buka ? \'display: flex;\' : \'display: none;\'}}"'
    c = c.replace(old_str, new_str)

c = c.replace('</style>', '  .time-inputs { display: flex; gap: 8px; margin-left: 12px; align-items: center; }\n  </style>')

with open('d:/Kuliah/Tugas Kuliah Semester 5/IPPL/Tugas Besar/Program Files/Cinemax/src/main/resources/templates/admin/pengaturan_admin.html', 'w', encoding='utf-8') as f: 
    f.write(c)

print('Patched successfully')
