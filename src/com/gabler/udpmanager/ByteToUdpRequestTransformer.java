package com.gabler.udpmanager;

import com.gabler.udpmanager.model.UdpRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.function.Function;

/**
 * Transform a byte array into a {@link UdpRequest}.
 *
 * @author Andy Gabler
 */
public class ByteToUdpRequestTransformer implements Function<byte[], UdpRequest> {

    @Override
    public UdpRequest apply(byte[] bytes) {
        final ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

        UdpRequest request;
        try {
            final ObjectInputStream objectInputStream = new ObjectInputStream(byteStream);
            final Object receivedRequest = objectInputStream.readObject();
            request = (UdpRequest) receivedRequest;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new RuntimeException(e);
        }

        return request;
    }
}
