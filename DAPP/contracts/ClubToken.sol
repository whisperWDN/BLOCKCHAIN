// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract ClubToken is ERC20, Ownable {
    uint256 public immutable claimAmount;
    mapping(address => bool) public hasClaimed;

    constructor(uint256 _claimAmount) ERC20("Club Governance Token", "CGT") {
        claimAmount = _claimAmount;
    }

    function claimInitialTokens() external {
        require(!hasClaimed[msg.sender], "Already claimed");
        hasClaimed[msg.sender] = true;
        _mint(msg.sender, claimAmount);
    }

    function mint(address to, uint256 amount) external onlyOwner {
        _mint(to, amount);
    }
}
