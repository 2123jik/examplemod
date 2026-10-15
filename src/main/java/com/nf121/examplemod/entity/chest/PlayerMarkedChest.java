package com.nf121.examplemod.entity.chest;

import java.util.UUID;

public interface PlayerMarkedChest {

    /**
     * 当前箱子是否被指定玩家标记。
     */
    boolean examplemod$isMarkedBy(UUID playerUuid);

    /**
     * 添加玩家标记。
     *
     * @return 集合是否发生变化
     */
    boolean examplemod$mark(UUID playerUuid);

    /**
     * 删除玩家标记。
     *
     * @return 集合是否发生变化
     */
    boolean examplemod$unmark(UUID playerUuid);
}
