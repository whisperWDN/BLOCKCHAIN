const fs = require("fs");
const path = require("path");
const { ethers, artifacts } = require("hardhat");

async function main() {
  const [deployer] = await ethers.getSigners();
  console.log("Deploying contracts with:", deployer.address);

  const claimAmount = ethers.parseEther("100");
  const proposalFee = ethers.parseEther("10");
  const minVoteAmount = ethers.parseEther("1");
  const proposalReward = ethers.parseEther("20");
  const proposalDuration = 3 * 24 * 60 * 60;

  const Token = await ethers.getContractFactory("ClubToken");
  const token = await Token.deploy(claimAmount);
  await token.waitForDeployment();

  const Souvenir = await ethers.getContractFactory("ClubSouvenir");
  const souvenir = await Souvenir.deploy("https://example.com/club-souvenir/");
  await souvenir.waitForDeployment();

  const Governance = await ethers.getContractFactory("ClubGovernance");
  const governance = await Governance.deploy(
    await token.getAddress(),
    await souvenir.getAddress(),
    proposalFee,
    minVoteAmount,
    proposalReward,
    proposalDuration
  );
  await governance.waitForDeployment();

  await (await token.transferOwnership(await governance.getAddress())).wait();
  await (await souvenir.transferOwnership(await governance.getAddress())).wait();

  console.log("ClubToken:", await token.getAddress());
  console.log("ClubSouvenir:", await souvenir.getAddress());
  console.log("ClubGovernance:", await governance.getAddress());

  const tokenArtifact = await artifacts.readArtifact("ClubToken");
  const govArtifact = await artifacts.readArtifact("ClubGovernance");
  const souvenirArtifact = await artifacts.readArtifact("ClubSouvenir");

  const frontendConfig = `window.DAPP_CONFIG = ${JSON.stringify(
    {
      chainId: 31337,
      contracts: {
        token: await token.getAddress(),
        governance: await governance.getAddress(),
        souvenir: await souvenir.getAddress()
      },
      abi: {
        token: tokenArtifact.abi,
        governance: govArtifact.abi,
        souvenir: souvenirArtifact.abi
      }
    },
    null,
    2
  )};\n`;

  const outputPath = path.join(__dirname, "..", "frontend", "js", "config.js");
  fs.writeFileSync(outputPath, frontendConfig);
  console.log("Frontend config written to:", outputPath);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
