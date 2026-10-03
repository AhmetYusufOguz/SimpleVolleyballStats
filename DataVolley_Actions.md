## Profesyonel Veri Sistemi (Data Volley Sembolleri)

Bu sistemde klavye veya arayüz üzerinden hızlı veri girmek için her kalite derecesine bir sembol atanmıştır. Toplamda 6 ana değerlendirme kriteri vardır:

* `#` : Mükemmel (Sayı getiren veya en ideal durum)
* `+` : Pozitif (İyi durum, avantajlı)
* `!` : Nötr / Taktiksel (Ne iyi ne kötü, oyun devam ediyor)
* `-` : Negatif (Kötü durum, avantaj rakibe geçti)
* `/` : Çok Kötü / Karşıya Geçen Top (Overpass)
* `=` : Hata (Doğrudan sayı kaybedildi)

### 1. Karşılama / Manşet (Reception)
* **Hata (=):** Servisten doğrudan sayı yenmesi (Ace).
* **Karşıya Geçen Top (/):** Manşetin doğrudan rakip sahaya gitmesi (Overpass).
* **Negatif (-):** Topun çok zor çıkarılması, pasörün 3 metre çizgisi dışından pas atmak zorunda kalması.
* **Nötr (!):** Pasörün yerinden çıkması ancak hâlâ 2 farklı hücumcuya pas verebilmesi (genelde "C Manşet" denir).
* **Pozitif (+):** Pasörün oldukça rahat olması ve en az 3 hücumcu opsiyonunu kullanabilmesi ("B Manşet").
* **Mükemmel (#):** Topun pasörün tam kafasına (ideal bölgeye) gitmesi, tüm hücum opsiyonlarının (ortadan hücum dahil) mükemmel işleyebilmesi ("A Manşet").

### 2. Servis (Serve)
* **Hata (=):** Servisin fileye takılması veya dışarı atılması.
* **Negatif (-):** Çok kolay bir servis atılması, rakibin mükemmel manşet alması.
* **Nötr (!):** Servisin rakibi biraz zorlaması ama oyun kurmalarına tam engel olamaması.
* **Pozitif (+):** Rakip pasörü ağdan uzaklaştıran ve rakibin hücum opsiyonlarını kısıtlayan servis.
* **Mükemmel (#):** Doğrudan sayı olan servis (Ace).

### 3. Hücum / Smaç (Attack)
* **Hata (=):** Topun fileye takılması veya anten/saha dışına atılması.
* **Bloklandı (/):** Hücumun rakip blok tarafından kesilip doğrudan sayıya dönüşmesi.
* **Negatif (-):** Rakibin çok kolay savunup kendi hücumunu rahatça organize ettiği yavaş/zayıf hücum.
* **Nötr (!):** Hücumun rakip bloktan sekip kendi sahamızda kalması (yeniden hücum şansı).
* **Pozitif (+):** Sayı olmayan ama rakibi çok zor bir savunmaya iten, rakibin etkili hücum yapmasını engelleyen smaç.
* **Mükemmel (#):** Doğrudan sayı olan hücum (Kill).

### 4. Savunma (Defense / Dig)
* **Hata (=):** Gelen smacın çıkarılamaması, topun yere düşmesi.
* **Karşıya Geçen Top (/):** Savunulan topun rakip sahaya hediye edilmesi (Free ball).
* **Negatif (-):** Topun havaya dikilmesi ancak hücum organize edilememesi.
* **Pozitif (+):** Zor bir smacın çıkarılarak etkili bir karşı hücum (counter-attack) fırsatı yaratılması.
* **Mükemmel (#):** İmkansız denilebilecek bir topun çıkarılarak pasöre mükemmel şekilde ulaştırılması.

### 5. Pas (Set)
* **Hata (=):** Taşıma, çift vuruş veya doğrudan hatalı pas.
* **Negatif (-):** Hücumcuyu blok karşısında çok kötü bir pozisyonda bırakan pas.
* **Pozitif (+):** Hücumcuya blok karşısında avantaj sağlayan rahat pas.
* **Mükemmel (#):** Hücumcuyu bloksuz bırakan veya çok net sayı şansı veren kusursuz pas.

### 6. Blok (Block)
* **Hata (=):** Fileye temas etme veya rakip sahaya geçme ihlali.
* **Negatif (-):** Rakibin bloktan faydalanarak (Block-out) sayı alması.
* **Nötr (!):** Topun bloktan sekerek oyunun devam etmesi (Soft block / Rebound).
* **Pozitif (+):** Topun bloktan rakip sahaya sekip onları zor durumda bırakması.
* **Mükemmel (#):** Doğrudan blokla sayı alınması (Stuff block).