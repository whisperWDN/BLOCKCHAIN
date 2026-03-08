const logEl = document.getElementById("log");
const accountEl = document.getElementById("account");
const tokenBalanceEl = document.getElementById("tokenBalance");
const passedCountEl = document.getElementById("passedCount");
const nftBalanceEl = document.getElementById("nftBalance");
const proposalListEl = document.getElementById("proposalList");

let provider;
let signer;
let account;
let token;
let governance;
let souvenir;

function appendLog(text) {
  const t = `[${new Date().toLocaleTimeString()}] ${text}`;
  logEl.textContent = `${t}\n${logEl.textContent}`;
}

function requireConnected() {
  if (!signer || !account) {
    throw new Error("请先连接钱包");
  }
}

function parseAmount(inputValue, fallback) {
  const value = inputValue.trim() || fallback;
  return ethers.parseEther(value);
}

async function connectWallet() {
  if (!window.ethereum) {
    alert("请安装 MetaMask");
    return;
  }

  provider = new ethers.BrowserProvider(window.ethereum);
  await provider.send("eth_requestAccounts", []);
  signer = await provider.getSigner();
  account = await signer.getAddress();

  const network = await provider.getNetwork();
  if (Number(network.chainId) !== Number(window.DAPP_CONFIG.chainId)) {
    appendLog(`当前链ID=${network.chainId}，建议切换到 ${window.DAPP_CONFIG.chainId}`);
  }

  token = new ethers.Contract(window.DAPP_CONFIG.contracts.token, window.DAPP_CONFIG.abi.token, signer);
  governance = new ethers.Contract(window.DAPP_CONFIG.contracts.governance, window.DAPP_CONFIG.abi.governance, signer);
  souvenir = new ethers.Contract(window.DAPP_CONFIG.contracts.souvenir, window.DAPP_CONFIG.abi.souvenir, signer);

  accountEl.textContent = account;
  appendLog("钱包已连接");
  await refreshAll();
}

async function refreshAll() {
  if (!account) return;

  const [tokenBalance, passedCount, nftBalance] = await Promise.all([
    token.balanceOf(account),
    governance.passedProposalCount(account),
    souvenir.balanceOf(account)
  ]);

  tokenBalanceEl.textContent = ethers.formatEther(tokenBalance);
  passedCountEl.textContent = passedCount.toString();
  nftBalanceEl.textContent = nftBalance.toString();
  await refreshProposals();
}

async function refreshProposals() {
  const count = Number(await governance.proposalCount());
  proposalListEl.innerHTML = "";

  for (let i = count; i >= 1; i -= 1) {
    const p = await governance.getProposal(i);
    const div = document.createElement("div");
    div.className = "proposal";
    div.innerHTML = `
      <strong>#${p[0]} ${p[2]}</strong><br>
      <small>提案人: ${p[1]}</small><br>
      <small>描述: ${p[3]}</small><br>
      <small>赞成: ${ethers.formatEther(p[4])} / 反对: ${ethers.formatEther(p[5])}</small><br>
      <small>截止: ${new Date(Number(p[7]) * 1000).toLocaleString()}</small><br>
      <small>已结算: ${p[8]} | 通过: ${p[9]} | 奖励已领: ${p[10]}</small>
    `;
    proposalListEl.appendChild(div);
  }
}

async function runTx(actionName, txPromiseFactory) {
  try {
    requireConnected();
    appendLog(`${actionName} 交易发送中...`);
    const tx = await txPromiseFactory();
    await tx.wait();
    appendLog(`${actionName} 成功: ${tx.hash}`);
    await refreshAll();
  } catch (error) {
    appendLog(`${actionName} 失败: ${error.shortMessage || error.message}`);
  }
}

document.getElementById("connectBtn").onclick = connectWallet;

document.getElementById("claimTokenBtn").onclick = () =>
  runTx("领取初始积分", async () => token.claimInitialTokens());

document.getElementById("approveProposalBtn").onclick = () =>
  runTx("授权提案费用", async () => {
    const amount = parseAmount(document.getElementById("approveProposalFee").value, "10");
    return token.approve(window.DAPP_CONFIG.contracts.governance, amount);
  });

document.getElementById("createProposalBtn").onclick = () =>
  runTx("创建提案", async () => {
    const title = document.getElementById("proposalTitle").value.trim();
    const desc = document.getElementById("proposalDesc").value.trim();
    if (!title || !desc) throw new Error("请填写标题和描述");
    return governance.createProposal(title, desc);
  });

document.getElementById("approveVoteBtn").onclick = () =>
  runTx("授权投票额度", async () => {
    const amount = parseAmount(document.getElementById("approveVoteAmount").value, "1");
    return token.approve(window.DAPP_CONFIG.contracts.governance, amount);
  });

document.getElementById("voteYesBtn").onclick = () =>
  runTx("赞成投票", async () => {
    const proposalId = Number(document.getElementById("voteProposalId").value);
    const amount = parseAmount(document.getElementById("voteAmount").value, "1");
    return governance.vote(proposalId, true, amount);
  });

document.getElementById("voteNoBtn").onclick = () =>
  runTx("反对投票", async () => {
    const proposalId = Number(document.getElementById("voteProposalId").value);
    const amount = parseAmount(document.getElementById("voteAmount").value, "1");
    return governance.vote(proposalId, false, amount);
  });

document.getElementById("finalizeBtn").onclick = () =>
  runTx("结算提案", async () => {
    const proposalId = Number(document.getElementById("voteProposalId").value);
    return governance.finalizeProposal(proposalId);
  });

document.getElementById("claimRewardBtn").onclick = () =>
  runTx("领取提案奖励", async () => {
    const proposalId = Number(document.getElementById("voteProposalId").value);
    return governance.claimProposalReward(proposalId);
  });

document.getElementById("claimSouvenirBtn").onclick = () =>
  runTx("领取纪念NFT", async () => governance.claimSouvenir());

document.getElementById("refreshBtn").onclick = async () => {
  try {
    await refreshAll();
    appendLog("已刷新");
  } catch (error) {
    appendLog(`刷新失败: ${error.shortMessage || error.message}`);
  }
};

