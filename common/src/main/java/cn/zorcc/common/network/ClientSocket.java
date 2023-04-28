package cn.zorcc.common.network;

import cn.zorcc.common.pojo.Loc;

/**
 *   Representing a socket accepted from remote client
 */
public record ClientSocket (
        Socket socket,
        Loc loc
){

}
