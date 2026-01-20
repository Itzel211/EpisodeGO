document.getElementById('search-form').addEventListener('submit', function(e) {
    e.preventDefault(); // 防止表單提交刷新頁面
    const query = document.getElementById('query').value.trim();
    const resultsDiv = document.getElementById('results');
	
    if (!query) {
        alert('請輸入搜尋關鍵字！');
        return;
    }
	
	// 顯示結果框
	resultsDiv.style.display = 'block';
	resultsDiv.innerHTML = '<p>搜尋中...</p>';

    fetch(`/api/search?q=${encodeURIComponent(query)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`伺服器錯誤：${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            resultsDiv.innerHTML = '';
            if (Object.keys(data).length === 0) {
                resultsDiv.innerHTML = '<p>沒有找到結果。</p>';
                return;
            }
            for (const [title, url] of Object.entries(data)) {
                const itemDiv = document.createElement('div');
                itemDiv.classList.add('result-item');

                const link = document.createElement('a');
                link.href = url;
                link.target = '_blank';
                link.rel = 'noopener noreferrer'; // 安全性設置
                link.textContent = title;

                // 只添加可點擊的標題，不再顯示 URL
                itemDiv.appendChild(link);
                resultsDiv.appendChild(itemDiv);
            }
        })
        .catch(error => {
            console.error('錯誤:', error);
            resultsDiv.innerHTML = '<p>發生錯誤，請稍後再試。</p>';
        });
});
