package com.hft.execution.feed;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesOut;

public class PriceEntry implements BytesMarshallable {
    double ask;
    double bid;
    double last;

    @Override
    public void readMarshallable(BytesIn<?> bytes) {
        ask  = bytes.readDouble();
        bid  = bytes.readDouble();
        last = bytes.readDouble();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void writeMarshallable(BytesOut bytes) {
        bytes.writeDouble(ask);
        bytes.writeDouble(bid);
        bytes.writeDouble(last);
    }
}
