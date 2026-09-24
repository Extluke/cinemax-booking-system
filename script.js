
      const formSection = document.getElementById('form-section');
      const formTitle = document.getElementById('form-title');
      const jadwalIdInput = document.getElementById('jadwal-id');
      const deleteIdInput = document.getElementById('delete-id');
      const btnDelete = document.getElementById('btn-delete');
      const btnSubmit = document.getElementById('btn-submit');
      
      const filmSelect = document.getElementById('film-select');
      const studioSelect = document.getElementById('studio-select');
      const dateInput = document.getElementById('show-date');
      const timeInput = document.getElementById('show-time');
      const priceInput = document.getElementById('ticket-price');
      const statusSelect = document.getElementById('schedule-status');

      function openAddForm() {
          formTitle.innerText = "Tambah Jadwal Tayang";
          jadwalIdInput.value = "";
          deleteIdInput.value = "";
          btnDelete.style.display = "none";
          btnSubmit.innerText = "Simpan & Terbitkan Jadwal";
          
          filmSelect.value = "";
          studioSelect.value = "";
          timeInput.value = "";
          priceInput.value = "35000";
          statusSelect.value = "AKTIF";
          
          formSection.style.display = "block";
          
          updatePreview();
      }

      function openEditForm(id, filmId, filmJudul, studioId, timeStr, harga, status) {
          formTitle.innerText = "Edit Jadwal: " + filmJudul;
          jadwalIdInput.value = id;
          deleteIdInput.value = id;
          btnDelete.style.display = "block";
          btnSubmit.innerText = "Simpan Perubahan";
          
          filmSelect.value = filmId;
          studioSelect.value = studioId;
          timeInput.value = timeStr;
          priceInput.value = harga;
          statusSelect.value = status;
          
          formSection.style.display = "block";
          
          updatePreview();
      }
      
      function closeForm() {
          formSection.style.display = "none";
          
          // Bersihkan live preview dari matriks utama
          const existingPreview = document.getElementById('live-preview-block');
          if (existingPreview) {
              existingPreview.remove();
          }
      }
      
      function updatePreview() {
          const filmId = filmSelect.value;
          const studioId = studioSelect.value;
          const date = dateInput.value;
          const time = timeInput.value;
          const jId = jadwalIdInput.value;
          
          // Bersihkan preview sebelumnya
          const existingPreview = document.getElementById('live-preview-block');
          if (existingPreview) {
              existingPreview.remove();
          }

          if(!filmId || !studioId || !date || !time) {
              return;
          }
          
          let url = `/admin/api/jadwal-preview?tanggal=${date}&jam=${time}&filmId=${filmId}&studioId=${studioId}`;
          if(jId) url += `&id=${jId}`;
          
          fetch(url)
            .then(res => res.json())
            .then(data => {
                if(data.error) return;
                
                const p = data.proposed;
                
                // Cari baris studio di matriks utama
                const targetRow = document.getElementById('timeline-row-' + studioId);
                if (targetRow) {
                    const pBlock = document.createElement('div');
                    pBlock.id = 'live-preview-block';
                    pBlock.style.position = 'absolute';
                    pBlock.style.top = '0';
                    pBlock.style.bottom = '0';
                    pBlock.style.borderRadius = '6px';
                    pBlock.style.padding = '8px';
                    pBlock.style.zIndex = '10';
                    pBlock.style.display = 'flex';
                    pBlock.style.flexDirection = 'column';
                    pBlock.style.justifyContent = 'center';
                    pBlock.style.left = p.leftPercentage + '%';
                    pBlock.style.width = p.widthPercentage + '%';
                    pBlock.style.boxShadow = '0 4px 12px rgba(0,0,0,0.3)';
                    pBlock.style.transition = 'all 0.3s';
                    
                    if(p.isConflict) {
                        // Desain khusus overlap
                        pBlock.style.background = 'repeating-linear-gradient(45deg, rgba(220, 53, 69, 0.8), rgba(220, 53, 69, 0.8) 10px, rgba(220, 53, 69, 1) 10px, rgba(220, 53, 69, 1) 20px)';
                        pBlock.style.color = '#fff';
                        pBlock.style.border = '2px solid #dc3545';
                        pBlock.innerHTML = `<strong>${p.filmJudul} (BENTROK)</strong><small>${p.waktuMulaiStr} — ${p.waktuSelesaiStr}</small><small style="background:#fff;color:#dc3545;padding:2px 4px;border-radius:4px;margin-top:4px;font-weight:bold;">${p.conflictReason}</small>`;
                        
                        btnSubmit.disabled = true;
                        btnSubmit.style.opacity = '0.5';
                    } else {
                        // Desain aman (hijau/biru)
                        pBlock.style.background = 'rgba(13, 110, 253, 0.9)';
                        pBlock.style.color = '#fff';
                        pBlock.style.border = '2px dashed #fff';
                        pBlock.innerHTML = `<strong>${p.filmJudul} (PREVIEW)</strong><small>${p.waktuMulaiStr} — ${p.waktuSelesaiStr}</small>`;
                        
                        btnSubmit.disabled = false;
                        btnSubmit.style.opacity = '1';
                    }
                    
                    targetRow.appendChild(pBlock);
                }
            });
      }
      
      filmSelect.addEventListener('change', updatePreview);
      studioSelect.addEventListener('change', updatePreview);
      dateInput.addEventListener('change', updatePreview);
      timeInput.addEventListener('change', updatePreview);
  