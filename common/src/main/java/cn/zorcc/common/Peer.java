package cn.zorcc.common;

import cn.zorcc.common.structure.Loc;

/**
 *   Peer represents a running node's information about it's Net server
 */
public record Peer(
        Long appId,
        Long nodeId,
        Loc loc,
        Integer weight,
        Long createdAt,
        Long modifiedAt
) {

}
