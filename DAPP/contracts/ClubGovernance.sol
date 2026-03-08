// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

interface IClubToken {
    function transferFrom(address from, address to, uint256 value) external returns (bool);
    function mint(address to, uint256 amount) external;
}

interface IClubSouvenir {
    function mintSouvenir(address to) external returns (uint256 tokenId);
}

contract ClubGovernance {
    struct Proposal {
        uint256 id;
        address proposer;
        string title;
        string description;
        uint256 yesVotes;
        uint256 noVotes;
        uint256 startTime;
        uint256 endTime;
        bool finalized;
        bool passed;
        bool rewardClaimed;
    }

    IClubToken public immutable token;
    IClubSouvenir public immutable souvenir;

    uint256 public immutable proposalFee;
    uint256 public immutable minVoteAmount;
    uint256 public immutable proposalReward;
    uint256 public immutable proposalDuration;

    uint256 public proposalCount;
    mapping(uint256 => Proposal) public proposals;
    mapping(uint256 => mapping(address => bool)) public hasVoted;

    mapping(address => uint256) public passedProposalCount;
    mapping(address => bool) public souvenirClaimed;

    event ProposalCreated(uint256 indexed proposalId, address indexed proposer, string title, uint256 endTime);
    event Voted(uint256 indexed proposalId, address indexed voter, bool support, uint256 amount);
    event ProposalFinalized(uint256 indexed proposalId, bool passed, uint256 yesVotes, uint256 noVotes);
    event RewardClaimed(uint256 indexed proposalId, address indexed proposer, uint256 rewardAmount);
    event SouvenirClaimed(address indexed student, uint256 tokenId);

    constructor(
        address tokenAddress,
        address souvenirAddress,
        uint256 _proposalFee,
        uint256 _minVoteAmount,
        uint256 _proposalReward,
        uint256 _proposalDuration
    ) {
        require(tokenAddress != address(0), "Invalid token");
        require(souvenirAddress != address(0), "Invalid souvenir");
        require(_proposalDuration > 0, "Invalid duration");

        token = IClubToken(tokenAddress);
        souvenir = IClubSouvenir(souvenirAddress);

        proposalFee = _proposalFee;
        minVoteAmount = _minVoteAmount;
        proposalReward = _proposalReward;
        proposalDuration = _proposalDuration;
    }

    function createProposal(string calldata title, string calldata description) external returns (uint256 proposalId) {
        require(bytes(title).length > 0, "Title required");
        require(bytes(description).length > 0, "Description required");
        require(token.transferFrom(msg.sender, address(this), proposalFee), "Fee transfer failed");

        proposalId = ++proposalCount;

        proposals[proposalId] = Proposal({
            id: proposalId,
            proposer: msg.sender,
            title: title,
            description: description,
            yesVotes: 0,
            noVotes: 0,
            startTime: block.timestamp,
            endTime: block.timestamp + proposalDuration,
            finalized: false,
            passed: false,
            rewardClaimed: false
        });

        emit ProposalCreated(proposalId, msg.sender, title, block.timestamp + proposalDuration);
    }

    function vote(uint256 proposalId, bool support, uint256 amount) external {
        Proposal storage p = proposals[proposalId];
        require(p.id != 0, "Proposal not found");
        require(block.timestamp <= p.endTime, "Voting ended");
        require(!hasVoted[proposalId][msg.sender], "Already voted");
        require(amount >= minVoteAmount, "Amount too low");
        require(token.transferFrom(msg.sender, address(this), amount), "Vote transfer failed");

        hasVoted[proposalId][msg.sender] = true;
        if (support) {
            p.yesVotes += amount;
        } else {
            p.noVotes += amount;
        }

        emit Voted(proposalId, msg.sender, support, amount);
    }

    function finalizeProposal(uint256 proposalId) public {
        Proposal storage p = proposals[proposalId];
        require(p.id != 0, "Proposal not found");
        require(block.timestamp > p.endTime, "Voting not ended");
        require(!p.finalized, "Already finalized");

        p.finalized = true;
        p.passed = p.yesVotes > p.noVotes;

        emit ProposalFinalized(proposalId, p.passed, p.yesVotes, p.noVotes);
    }

    function claimProposalReward(uint256 proposalId) external {
        Proposal storage p = proposals[proposalId];
        require(p.id != 0, "Proposal not found");
        if (!p.finalized && block.timestamp > p.endTime) {
            finalizeProposal(proposalId);
        }

        require(p.proposer == msg.sender, "Not proposer");
        require(p.finalized, "Not finalized");
        require(p.passed, "Proposal not passed");
        require(!p.rewardClaimed, "Reward already claimed");

        p.rewardClaimed = true;
        passedProposalCount[msg.sender] += 1;
        token.mint(msg.sender, proposalReward);

        emit RewardClaimed(proposalId, msg.sender, proposalReward);
    }

    function claimSouvenir() external {
        require(!souvenirClaimed[msg.sender], "Already claimed souvenir");
        require(passedProposalCount[msg.sender] >= 3, "Need 3 passed proposals");

        souvenirClaimed[msg.sender] = true;
        uint256 tokenId = souvenir.mintSouvenir(msg.sender);

        emit SouvenirClaimed(msg.sender, tokenId);
    }

    function getProposal(uint256 proposalId)
        external
        view
        returns (
            uint256 id,
            address proposer,
            string memory title,
            string memory description,
            uint256 yesVotes,
            uint256 noVotes,
            uint256 startTime,
            uint256 endTime,
            bool finalized,
            bool passed,
            bool rewardClaimed
        )
    {
        Proposal memory p = proposals[proposalId];
        require(p.id != 0, "Proposal not found");

        return (
            p.id,
            p.proposer,
            p.title,
            p.description,
            p.yesVotes,
            p.noVotes,
            p.startTime,
            p.endTime,
            p.finalized,
            p.passed,
            p.rewardClaimed
        );
    }
}
