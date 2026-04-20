const output = document.getElementById("output");
const jwtTokenInput = document.getElementById("jwtToken");

function printResult(title, payload) {
  output.textContent = `${title}\n${JSON.stringify(payload, null, 2)}`;
}

function getToken() {
  return jwtTokenInput.value.trim();
}

function buildHeaders(extra = {}) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    ...extra,
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function apiRequest(path, { method = "GET", body, headers = {} } = {}) {
  const response = await fetch(path, {
    method,
    headers: buildHeaders(headers),
    body: body ? JSON.stringify(body) : undefined,
  });

  let data;
  try {
    data = await response.json();
  } catch (e) {
    data = { message: await response.text() };
  }

  if (!response.ok) {
    throw {
      status: response.status,
      headers: Object.fromEntries(response.headers.entries()),
      data,
    };
  }

  return data;
}

function randomIdempotencyKey() {
  if (window.crypto && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

document.getElementById("tokenForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const userId = Number(document.getElementById("tokenUserId").value);

  try {
    const data = await apiRequest("/api/v1/auth/demo-token", {
      method: "POST",
      body: { userId },
    });
    jwtTokenInput.value = data.token || "";
    printResult("Token Uretildi", data);
  } catch (err) {
    printResult("Token Hatasi", err);
  }
});

document.getElementById("createWalletForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const userId = Number(document.getElementById("createUserId").value);
  const currency = document.getElementById("createCurrency").value.trim().toUpperCase();

  try {
    const data = await apiRequest("/api/v1/wallets", {
      method: "POST",
      body: { userId, currency },
    });
    printResult("Cuzdan Olusturuldu", data);
  } catch (err) {
    printResult("Cuzdan Olusturma Hatasi", err);
  }
});

document.getElementById("depositForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const amount = Number(document.getElementById("depositAmount").value);

  try {
    const data = await apiRequest("/api/v1/wallets/deposit", {
      method: "POST",
      body: { amount },
    });
    printResult("Deposit Basarili", data);
  } catch (err) {
    printResult("Deposit Hatasi", err);
  }
});

document.getElementById("withdrawForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const amount = Number(document.getElementById("withdrawAmount").value);

  try {
    const data = await apiRequest("/api/v1/wallets/withdraw", {
      method: "POST",
      body: { amount },
    });
    printResult("Withdraw Basarili", data);
  } catch (err) {
    printResult("Withdraw Hatasi", err);
  }
});

document.getElementById("transferForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const toUserId = Number(document.getElementById("toUserId").value);
  const amount = Number(document.getElementById("transferAmount").value);
  const keyInput = document.getElementById("idempotencyKey").value.trim();
  const idempotencyKey = keyInput || randomIdempotencyKey();

  try {
    const data = await apiRequest("/api/v1/wallets/transfer", {
      method: "POST",
      headers: { "Idempotency-Key": idempotencyKey },
      body: { toUserId, amount },
    });
    printResult("Transfer Basarili", { idempotencyKey, ...data });
  } catch (err) {
    printResult("Transfer Hatasi", { idempotencyKey, ...err });
  }
});

document.getElementById("getWalletBtn").addEventListener("click", async () => {
  try {
    const data = await apiRequest("/api/v1/wallets");
    printResult("Wallet", data);
  } catch (err) {
    printResult("Wallet Hatasi", err);
  }
});

document.getElementById("transactionsForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const walletId = Number(document.getElementById("walletId").value);
  const sortBy = document.getElementById("sortBy").value;
  const direction = document.getElementById("direction").value;
  const size = Number(document.getElementById("size").value);

  const url = `/api/v1/wallets/${walletId}/transactions?page=0&size=${size}&sortBy=${sortBy}&direction=${direction}`;

  try {
    const data = await apiRequest(url);
    printResult("Transactions", data);
  } catch (err) {
    printResult("Transactions Hatasi", err);
  }
});

