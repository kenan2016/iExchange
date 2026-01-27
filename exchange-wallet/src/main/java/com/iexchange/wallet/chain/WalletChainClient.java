package com.iexchange.wallet.chain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;
import java.math.RoundingMode;

/**
 * 链上交互客户端（ERC20）。
 */
@Slf4j
@Service
public class WalletChainClient {

    private static final Event TRANSFER_EVENT = new Event("Transfer",
        Arrays.asList(
            new TypeReference<Address>(true) {
            },
            new TypeReference<Address>(true) {
            },
            new TypeReference<Uint256>() {
            }
        ));

    private final WalletChainProperties properties;
    private final WalletChainConfigService configService;
    private final Web3j web3j;

    public WalletChainClient(WalletChainProperties properties, WalletChainConfigService configService) {
        this.properties = properties;
        this.configService = configService;
        String rpcUrl = normalize(properties.getRpcUrl());
        if (rpcUrl == null) {
            throw new IllegalArgumentException("链上 RPC 未配置");
        }
        this.web3j = Web3j.build(new HttpService(rpcUrl));
    }

    public BigInteger getLatestBlockNumber() {
        try {
            EthBlockNumber response = web3j.ethBlockNumber().send();
            return response.getBlockNumber();
        } catch (Exception ex) {
            throw new IllegalArgumentException("获取链上区块高度失败", ex);
        }
    }

    public List<Log> getTransferLogs(BigInteger fromBlock, BigInteger toBlock) {
        String tokenAddress = requireTokenAddress();
        String topic = EventEncoder.encode(TRANSFER_EVENT);
        EthFilter filter = new EthFilter(
            DefaultBlockParameter.valueOf(fromBlock),
            DefaultBlockParameter.valueOf(toBlock),
            tokenAddress);
        filter.addSingleTopic(topic);
        try {
            EthLog logs = web3j.ethGetLogs(filter).send();
            return logs.getLogs().stream()
                .map(log -> (Log) log.get())
                .toList();
        } catch (Exception ex) {
            throw new IllegalArgumentException("获取链上日志失败", ex);
        }
    }

    public String sendTokenTransfer(String toAddress, BigDecimal amount) {
        Credentials credentials = credentialsFromPrivateKey(configService.resolvePrivateKey());
        return sendTokenTransferWithCredentials(credentials, toAddress, amount);
    }

    public String sendTokenTransferFrom(String fromPrivateKey, String toAddress, BigDecimal amount) {
        Credentials credentials = credentialsFromPrivateKey(fromPrivateKey);
        return sendTokenTransferWithCredentials(credentials, toAddress, amount);
    }

    public BigDecimal getTokenBalance(String address) {
        String tokenAddress = requireTokenAddress();
        Function function = new Function(
            "balanceOf",
            Collections.singletonList(new Address(address)),
            Collections.singletonList(new TypeReference<Uint256>() {
            }));
        String data = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(null, tokenAddress, data);
        try {
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            if (response.hasError()) {
                throw new IllegalArgumentException(response.getError().getMessage());
            }
            List<Type> values = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
            if (values.isEmpty()) {
                return BigDecimal.ZERO;
            }
            BigInteger raw = (BigInteger) values.get(0).getValue();
            return toDecimalAmount(raw);
        } catch (Exception ex) {
            throw new IllegalArgumentException("获取链上余额失败", ex);
        }
    }

    public EthGetTransactionReceipt getReceipt(String txHash) {
        try {
            return web3j.ethGetTransactionReceipt(txHash).send();
        } catch (Exception ex) {
            throw new IllegalArgumentException("获取交易回执失败", ex);
        }
    }

    public Web3j getWeb3j() {
        return web3j;
    }

    public BigDecimal toDecimalAmount(BigInteger value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value).movePointLeft(properties.getToken().getDecimals());
    }

    public BigInteger toTokenAmount(BigDecimal amount) {
        if (amount == null) {
            return BigInteger.ZERO;
        }
        return amount.movePointRight(properties.getToken().getDecimals())
            .setScale(0, RoundingMode.DOWN)
            .toBigIntegerExact();
    }

    public String requireTokenAddress() {
        String address = normalize(properties.getToken().getAddress());
        if (address == null) {
            throw new IllegalArgumentException("Token 合约地址未配置");
        }
        return address;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sendTokenTransferWithCredentials(Credentials credentials, String toAddress, BigDecimal amount) {
        String tokenAddress = requireTokenAddress();
        BigInteger value = toTokenAmount(amount);
        Function function = new Function(
            "transfer",
            Arrays.asList(new Address(toAddress), new Uint256(value)),
            Collections.emptyList());
        String data = FunctionEncoder.encode(function);
        try {
            EthGetTransactionCount txCount = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.PENDING).send();
            BigInteger nonce = txCount.getTransactionCount();
            EthGasPrice gasPrice = web3j.ethGasPrice().send();
            BigInteger gasLimit = BigInteger.valueOf(150_000);
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice.getGasPrice(),
                gasLimit,
                tokenAddress,
                BigInteger.ZERO,
                data);
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, properties.getChainId(), credentials);
            EthSendTransaction response = web3j.ethSendRawTransaction(Numeric.toHexString(signedMessage)).send();
            if (response.hasError()) {
                throw new IllegalArgumentException(response.getError().getMessage());
            }
            return response.getTransactionHash();
        } catch (Exception ex) {
            throw new IllegalArgumentException("链上转账失败", ex);
        }
    }

    private Credentials credentialsFromPrivateKey(String privateKey) {
        String normalized = normalize(privateKey);
        if (normalized == null) {
            throw new IllegalArgumentException("私钥不能为空");
        }
        String key = normalized.startsWith("0x") || normalized.startsWith("0X")
            ? normalized.substring(2)
            : normalized;
        return Credentials.create(key);
    }
}
