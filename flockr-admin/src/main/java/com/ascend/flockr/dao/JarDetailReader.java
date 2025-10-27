package com.ascend.flockr.dao;

import com.ascend.flockr.model.task.JarDetail;
import io.reactivex.Maybe;

public interface JarDetailReader {

  Maybe<String> latestJar();

  Maybe<JarDetail> latestJarDetails();
}
