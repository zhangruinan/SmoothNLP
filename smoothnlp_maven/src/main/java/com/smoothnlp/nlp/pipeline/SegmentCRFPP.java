package com.smoothnlp.nlp.pipeline;

import com.smoothnlp.nlp.SmoothNLP;
import com.smoothnlp.nlp.model.crfpp.*;

import com.smoothnlp.nlp.basic.*;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SegmentCRFPP extends CRFModel implements ISequenceTagger {

    protected ModelImpl model;
    private static String STOP_LABEL = "S";
    private static String BLANK_LABEL = "B";
    private SDictionary dictionary;

    public SegmentCRFPP(){
        this.model = new ModelImpl();
        this.model.open(SmoothNLP.CRF_SEGMENT_MODEL,0,0,1.0);
        this.dictionary = new SDictionary(SmoothNLP.libraries);
    }

    public List<SToken> process(String input){
        Tagger tagger = this.model.createTagger();
        if (tagger==null){
            SmoothNLP.LOGGER.severe(String.format("CRF segment model is not properly read"));
        }
        if (input == null || input.length() == 0) {
            return new ArrayList<SToken>();
        }
        char[] chars = input.toCharArray();
        for (char c: chars) {
            String ftrs = super.buildFtrs(c);  // Build ftrs for crf needed sequence, in this case, only each char is needed
            tagger.add(ftrs);
        }
        tagger.parse();
        StringBuilder temToken = new StringBuilder();
        List<SToken> resTokens = new ArrayList<SToken>();

        String[] ytags = new String[tagger.size()];

        // get stop/blank tags from crf model
        for (int i = 0; i < tagger.size(); i++){
            ytags[i] = tagger.yname(tagger.y(i));
        }

        // get stop/blank tags by matching keywords
        List<int[]> matchedRanges = this.dictionary.indicate(input);
        for (int[] range: matchedRanges){
            int start = range[0];
            int end = range[1];
            for (int j = start; j<end; j++){
                ytags[j] = BLANK_LABEL;
            }
            ytags[end-1] = STOP_LABEL;
        }

        // build tokens
        for (int i =0; i<tagger.size();i++){
            temToken.append(chars[i]);
            if (ytags[i].equals(STOP_LABEL)) {
                resTokens.add(new SToken(temToken.toString()));
                temToken = new StringBuilder();
            }
        }

        return resTokens;

    }

    private class SDictionary{
        private List<String> wordList;
        private Pattern patterns;

        public SDictionary(String[] args){
            wordList = new ArrayList<>();
            for (int i = 0; i<args.length;i=i+1){
                String fileName = args[i];
                try {
                    InputStream is = SmoothNLP.IOAdaptor.open(fileName);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    while(reader.ready()) {
                        String word = reader.readLine();
                        wordList.add(word);
                    }
                }catch(IOException e){
                    SmoothNLP.LOGGER.severe(e.getMessage());
                }
            }
            patterns = Pattern.compile(UtilFns.join("|",wordList));
        }

        public List<int[]> indicate(String inputText){
            List<int[]> resList = new ArrayList<>();
            Matcher matcher = patterns.matcher(inputText);
            while (matcher.find()){
                resList.add(new int[]{matcher.start(),matcher.end()});
            }
            return resList;
        }

    }

    public static void main(String[] args){
        SegmentCRFPP s = new SegmentCRFPP();
        System.out.println(s.process("国泰君安是一家资本公司"));
    }

}
