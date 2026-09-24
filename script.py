import os, glob
count = 0
for f in glob.glob('d:/Kuliah/Tugas Kuliah Semester 5/IPPL/Tugas Besar/Program Files/Cinemax/src/main/resources/templates/**/*.html', recursive=True):
    with open(f, 'r', encoding='utf-8') as file: content = file.read()
    if 'CINE<span>PLEX</span>' in content:
        content = content.replace('CINE<span>PLEX</span>', '<span th:text="${appConfig != null ? #strings.toUpperCase(appConfig.namaBioskop) : ''CINEPLEX''}">CINEPLEX</span>')
        with open(f, 'w', encoding='utf-8') as file: file.write(content)
        count += 1
print(f'Replaced in {count} files')
