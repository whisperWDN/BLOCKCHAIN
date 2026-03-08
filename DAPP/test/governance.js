const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("ClubGovernance", function () {
  it("allows create vote finalize reward", async function () {
    const [alice, bob] = await ethers.getSigners();

    const Token = await ethers.getContractFactory("ClubToken");
    const token = await Token.deploy(ethers.parseEther("100"));

    const Souvenir = await ethers.getContractFactory("ClubSouvenir");
    const souvenir = await Souvenir.deploy("https://example.com/");

    const Governance = await ethers.getContractFactory("ClubGovernance");
    const gov = await Governance.deploy(
      await token.getAddress(),
      await souvenir.getAddress(),
      ethers.parseEther("10"),
      ethers.parseEther("1"),
      ethers.parseEther("20"),
      3600
    );

    await token.transferOwnership(await gov.getAddress());
    await souvenir.transferOwnership(await gov.getAddress());

    await token.connect(alice).claimInitialTokens();
    await token.connect(bob).claimInitialTokens();

    await token.connect(alice).approve(await gov.getAddress(), ethers.parseEther("20"));
    await gov.connect(alice).createProposal("Trip", "Weekend trip voting");

    await token.connect(bob).approve(await gov.getAddress(), ethers.parseEther("5"));
    await gov.connect(bob).vote(1, true, ethers.parseEther("2"));

    await ethers.provider.send("evm_increaseTime", [3601]);
    await ethers.provider.send("evm_mine", []);

    await gov.finalizeProposal(1);
    await gov.connect(alice).claimProposalReward(1);

    const passed = await gov.passedProposalCount(alice.address);
    expect(passed).to.equal(1);
  });
});
