package org.hyperledger.fabric.chaincode;

import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class basicchaincode extends ChaincodeBase{
    private static Log _logger = LogFactory.getLog(basicchaincode.class);

    @Override
    public Response init(ChaincodeStub stub) {
        try {
            List<String> args = stub.getStringArgs();
            if(args.size() != 2) {
                newErrorResponse("Error Incorrect arguments");
            }
            stub.putStringState(args.get(0), args.get(1));
            return newSuccessResponse();
        } catch (Throwable e) {
            return newErrorResponse("Failed to create asset");
        }
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        try {
            String func = stub.getFunction();
            List<String> params = stub.getParameters();

            if(func.equals("set")) {
                return set(stub, params);
            } else if(func.equals("transfer")) {
                return transfer(stub, params);
            } else if (func.equals("get")) {
                return get(stub, params);
            }
            return newErrorResponse("Invalid invoke function name. Expecting one of: [\"set\",\"get\"");
        } catch (Throwable e) {
            return newErrorResponse(e.getMessage());
        }
    }
    private Response get(ChaincodeStub stub, List<String> args) {
        if(args.size() != 1) {
            return newErrorResponse("Incorrect number of arguments. Expoecting name of the person to query");
        }
        String key = args.get(0);
        String val = stub.getStringState(key);

        if(val == null) {
            return newErrorResponse(String.format("Error: state for %s is null", key));
        }
        return newSuccessResponse(val, ByteString.copyFrom(val, UTF_8).toByteArray());
    }

    private Response set(ChaincodeStub stub, List<String> args) {
        if(args.size() != 2) {
            throw new RuntimeException("Error Incorrect arguments");
        }
        stub.putStringState(args.get(0), args.get(1));
        return newSuccessResponse(args.get(1));
    }
    private Response transfer(ChaincodeStub stub, List<String> args) {
        if(args.size() != 3) {
            return newErrorResponse("Incorrect number of arguments. Expecting 3");
        }
        String accountFromKey = args.get(0);
        String accountToKey = args.get(1);

        String accountFromValueStr = stub.getStringState(accountFromKey);
        if(accountFromValueStr == null) {
            return newErrorResponse(String.format("Entity %s not found", accountFromKey));
        }
        int accountFromValue = Integer.parseInt(accountFromValueStr);

        String accountToValueStr = stub.getStringState(accountToKey);
        if(accountToValueStr == null) {
            return newErrorResponse(String.format("Entity %s not found", accountToKey));
        }
        int accountToValue = Integer.parseInt(accountToValueStr);

        int amount = Integer.parseInt(args.get(2));

        if(amount > accountFromValue) {
            return newErrorResponse(String.format("not enough money in account %s", accountFromKey));
        }

        accountFromValue -= amount;
        accountToValue += amount;

        _logger.info(String.format("new value of A: %s", accountFromValue));
        _logger.info(String.format("new value of B: %s", accountToValue));

        stub.putStringState(accountFromKey, Integer.toString(accountFromValue));
        stub.putStringState(accountToKey, Integer.toString(accountToValue));

        _logger.info("Transfer complete");

        return newSuccessResponse("invoke finished successfully", ByteString.copyFrom(accountFromKey+
                ":"+accountFromValue+" "+accountToKey+":"+accountToValue,UTF_8).toByteArray());
    }

    public static void main(String[] args) {
        new basicchaincode().start(args);
    }

}
