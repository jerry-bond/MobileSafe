package cn.jet.mobilesafe;

/**
 * Created by jerry on 2/26/2016.
 */
public interface Observer {
    static final int mExitType = 1;

    public void notify(Subject subject, int nType);
}