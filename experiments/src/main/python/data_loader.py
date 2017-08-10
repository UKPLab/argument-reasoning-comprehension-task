import gzip
import os.path
from typing import Dict, Tuple, List

import vocabulary_embeddings_extractor


def string_to_indices(string: str, word_to_indices_map_param: Dict, nb_words: int = None) -> List:
    """
    Tokenizes a string and converts to indices specified in word_to_indices_map; performs also OOV replacement
    :param string: string
    :param word_to_indices_map_param: map (word index, embedding index)
    :param nb_words: all words with higher index are treated as OOV
    :return: a list of indices
    """
    tokens = vocabulary_embeddings_extractor.tokenize(string)

    # now convert tokens to indices; set to 2 for OOV
    word_indices_list = [word_to_indices_map_param.get(word, 2) for word in tokens]

    # limit words to max nb_words (set them to OOV = 2):
    if nb_words:
        word_indices_list = [2 if word_index >= nb_words else word_index for word_index in word_indices_list]

    return word_indices_list


def load_single_instance_from_line(line: str, word_to_indices_map: Dict, nb_words: int = None) -> Tuple:
    """
    Load a single training/test instance from a single line int tab-separated format
    :param line: string line
    :param word_to_indices_map:  map (word index, embedding index)
    :param nb_words: all words with higher index are treated as OOV
    :return: tuple (instance_id, warrant0, warrant1, correct_label_w0_or_w1, reason, claim, debate_meta_data)
    """
    split_line = line.split('\t')
    # "#id warrant0 warrant1 correctLabelW0orW1 reason claim debateTitle debateInfo
    assert len(split_line) == 8

    instance_id = split_line[0]
    warrant0 = string_to_indices(split_line[1], word_to_indices_map, nb_words)
    warrant1 = string_to_indices(split_line[2], word_to_indices_map, nb_words)
    correct_label_w0_or_w1 = int(split_line[3])
    reason = string_to_indices(split_line[4], word_to_indices_map, nb_words)
    claim = string_to_indices(split_line[5], word_to_indices_map, nb_words)
    debate_title = string_to_indices(split_line[6], word_to_indices_map, nb_words)
    debate_info = string_to_indices(split_line[7], word_to_indices_map, nb_words)
    # concatenate these two into one vector
    debate_meta_data = debate_title + debate_info

    # make sure everything is filled
    assert len(instance_id) > 0
    assert len(warrant0) > 0
    assert len(warrant1) > 0
    assert correct_label_w0_or_w1 == 0 or correct_label_w0_or_w1 == 1
    assert len(reason) > 0
    assert len(claim) > 0
    assert len(debate_meta_data) > 0

    return instance_id, warrant0, warrant1, correct_label_w0_or_w1, reason, claim, debate_meta_data


def load_single_file(file_name: str, word_to_indices_map: Dict, nb_words: int = None) -> Tuple:
    """
    Loads a single train/test file and returns a tuple of lists
    :param file_name: full file name
    :param word_to_indices_map: vocabulary map in form {word: index} where index also correspond to frequencies
    :param nb_words: if a particular word index is higher than this value, the word is treated as OOV
    :return: instance_id_list = list of strings
        warrant0_list = list of list of word indices (where integer is a word index taken from word_to_indices_map)
        warrant1_list = list of list of word indices
        correct_label_w0_or_w1_list = list of 0 or 1
        reason_list = list of list of word indices
        claim_list = list of list of word indices
        debate_meta_data_list = list of list of word indices
    """
    assert len(word_to_indices_map) > 0

    if file_name.endswith('gz'):
        f = gzip.open(file_name, 'rb')
    else:
        f = open(file_name, 'r')
    lines = f.readlines()
    # remove first line with comments
    del lines[0]

    instance_id_list = []
    warrant0_list = []
    warrant1_list = []
    correct_label_w0_or_w1_list = []
    reason_list = []
    claim_list = []
    debate_meta_data_list = []

    for line in lines:
        # convert to vectors of embedding indices where appropriate
        instance_id, warrant0, warrant1, correct_label_w0_or_w1, reason, claim, debate_meta_data = \
            load_single_instance_from_line(line, word_to_indices_map, nb_words)

        # add to the result
        instance_id_list.append(instance_id)
        warrant0_list.append(warrant0)
        warrant1_list.append(warrant1)
        correct_label_w0_or_w1_list.append(correct_label_w0_or_w1)
        reason_list.append(reason)
        claim_list.append(claim)
        debate_meta_data_list.append(debate_meta_data)

    return (instance_id_list, warrant0_list, warrant1_list, correct_label_w0_or_w1_list, reason_list, claim_list,
            debate_meta_data_list)


def __main__():
    current_dir = os.getcwd()
    embeddings_cache_file = current_dir + "/embeddings_cache_file_word2vec.pkl.bz2"

    # load pre-extracted word-to-index maps and pre-filtered Glove embeddings
    word_to_indices_map, word_index_to_embeddings_map = \
        vocabulary_embeddings_extractor.load_cached_vocabulary_and_embeddings(embeddings_cache_file)

    load_single_file(current_dir + '/data/train.tsv', word_to_indices_map)


if __name__ == "__main__":
    __main__()

pass
