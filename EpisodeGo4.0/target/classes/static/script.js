document.getElementById('search-form').addEventListener('submit', function (e) {
    e.preventDefault(); // 防止表單提交刷新頁面

    const queryInput = document.getElementById('query');
    const query = queryInput.value.trim();
    const resultsDiv = document.getElementById('results');

    // 若未輸入搜尋關鍵字
    if (!query) {
        alert('請輸入搜尋關鍵字！');
        return;
    }

    // 顯示結果區域，並提示「搜尋中...」
    resultsDiv.style.display = 'block';
    resultsDiv.innerHTML = `
        <p>搜尋中
        <br><br><br>
            <img src="loading.gif" alt="Loading" style="width: 60px; height: 60px; vertical-align: middle;" />
        </p>
    `;


    // 向後端發送請求
    fetch(`/api/search?q=${encodeURIComponent(query)}`)
        .then(response => {
            if (!response.ok) {
                // 若伺服器回傳非 2xx 狀態，拋出錯誤
                throw new Error(`伺服器錯誤：${response.status}`);
            }
            return response.json(); // 解析 JSON
        })
        .then(data => {
            // 清空目前顯示區域
            resultsDiv.innerHTML = '';

            // 若後端回傳空物件，表示沒有結果
            if (Object.keys(data).length === 0) {
                resultsDiv.innerHTML = '<p>沒有找到相關結果。 請重新輸入正確關鍵字</p>';
                return;
            }

            // 根據後端回傳的 Map<標題, URL> 進行顯示
            for (const [title, url] of Object.entries(data)) {
                const itemDiv = document.createElement('div');
                itemDiv.classList.add('result-item');

                const link = document.createElement('a');
                link.href = url;
                link.target = '_blank';               // 新視窗/標籤打開
                link.rel = 'noopener noreferrer';      // 安全性設置
                link.textContent = title;              // 顯示連結文字為標題

                // 僅顯示可點擊的標題
                itemDiv.appendChild(link);
                resultsDiv.appendChild(itemDiv);
            }
        })
        .catch(error => {
            console.error('錯誤:', error);
            resultsDiv.innerHTML = '<p>發生錯誤，請稍後再試。</p>';
        });
});
