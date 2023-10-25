package cn.zorcc.common.network;

import cn.zorcc.common.Carrier;

/**
 *   Network tagged msg
 *   Int tag should work for 99.99% cases, if you think there is still a tag overflow potential, try using other technics to solve it
*/
public record TaggedMsg(
        int tag,
        Carrier carrier
) {

}
