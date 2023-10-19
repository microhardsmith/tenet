package cn.zorcc.common.network;

import cn.zorcc.common.structure.Loc;

/**
 *   Representing a socket accepted from remote client
 */
public record ClientSocket (
        Socket socket,
        Loc loc
){

}
