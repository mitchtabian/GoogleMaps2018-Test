package com.codingwithmitch.googledirectionstest;

public interface ICallback {

    void done(Exception e);

    void done(Throwable e);
}
