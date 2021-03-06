// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.language.simple;

import com.google.inject.Inject;
import com.yahoo.collections.Tuple2;
import com.yahoo.component.Version;
import com.yahoo.language.Linguistics;
import com.yahoo.language.detect.Detector;
import com.yahoo.language.process.CharacterClasses;
import com.yahoo.language.process.GramSplitter;
import com.yahoo.language.process.Normalizer;
import com.yahoo.language.process.Segmenter;
import com.yahoo.language.process.SegmenterImpl;
import com.yahoo.language.process.Stemmer;
import com.yahoo.language.process.StemmerImpl;
import com.yahoo.language.process.Tokenizer;
import com.yahoo.language.process.Transformer;

/**
 * Factory of pure Java linguistic processor implementations.
 *
 * @author bratseth
 * @author bjorncs
 */
public class SimpleLinguistics implements Linguistics {

    // Threadsafe instances
    private final Normalizer normalizer;
    private final Transformer transformer;
    private final Detector detector;
    private final CharacterClasses characterClasses;
    private final GramSplitter gramSplitter;

    @Inject
    @SuppressWarnings("deprecation")
    public SimpleLinguistics() {
        this(true);

    }

    /** @deprecated use OpenNlpLinguistics to get optimaize */
    @Deprecated // OK
    public SimpleLinguistics(boolean enableOptimaize) {
        this(new SimpleDetector(enableOptimaize));
    }

    /** @deprecated use OpenNlpLinguistics to get optimaize */
    @Deprecated // OK
    public SimpleLinguistics(SimpleLinguisticsConfig config) {
        this(new SimpleDetector(config.detector()));
    }

    private SimpleLinguistics(Detector detector) {
        this.normalizer = new SimpleNormalizer();
        this.transformer = new SimpleTransformer();
        this.detector = detector;
        this.characterClasses = new CharacterClasses();
        this.gramSplitter = new GramSplitter(characterClasses);
    }

    @Override
    public Stemmer getStemmer() { return new StemmerImpl(getTokenizer()); }

    @Override
    public Tokenizer getTokenizer() { return new SimpleTokenizer(normalizer, transformer); }

    @Override
    public Normalizer getNormalizer() { return normalizer; }

    @Override
    public Transformer getTransformer() { return transformer; }

    @Override
    public Segmenter getSegmenter() { return new SegmenterImpl(getTokenizer()); }

    @Override
    public Detector getDetector() { return detector; }

    @Override
    public GramSplitter getGramSplitter() { return gramSplitter; }

    @Override
    public CharacterClasses getCharacterClasses() { return characterClasses; }

    /** @deprecated do not use */
    @Deprecated // OK
    @Override
    public Tuple2<String, Version> getVersion(Component component) {
        return new Tuple2<>("yahoo", new Version(1, 0));
    }

}
