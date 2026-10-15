package com.nf121.examplemod.util;

public interface IStructureScanner {
    /**
     * 手动触发遍历扫描所有结构，校验 GZIP/NBT，并将损坏文件直接放入黑名单
     * @return 扫描到的损坏结构数量
     */
    int scanAndCacheCorruptStructures();
}