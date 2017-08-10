import os

import numpy as np
from keras.preprocessing import sequence
from keras.utils.np_utils import accuracy

import data_loader
import vocabulary_embeddings_extractor
from models import get_attention_lstm, get_attention_lstm_intra_warrant


def get_predicted_labels(predicted_probabilities):
    """
    Converts predicted probability/ies to label(s)
    @param predicted_probabilities: output of the classifier
    @return: labels as integers
    """
    assert isinstance(predicted_probabilities, np.ndarray)

    # if the output vector is a probability distribution, return the maximum value; otherwise
    # it's sigmoid, so 1 or 0
    if predicted_probabilities.shape[-1] > 1:
        predicted_labels_numpy = predicted_probabilities.argmax(axis=-1)
    else:
        predicted_labels_numpy = np.array([1 if p > 0.5 else 0 for p in predicted_probabilities])

    # check type
    assert isinstance(predicted_labels_numpy, np.ndarray)
    # convert to a Python list of integers
    predicted_labels = predicted_labels_numpy.tolist()
    assert isinstance(predicted_labels, list)
    assert isinstance(predicted_labels[0], int)
    # check it matches the gold labels

    return predicted_labels


def __main__():
    # optional (and default values)
    verbose = 1

    lstm_size = 64
    dropout = 0.9  # empirically tested on dev set
    nb_epoch = 5  # empirically tested on dev set
    max_len = 100  # padding length
    batch_size = 32

    print('Loading data...')

    current_dir = os.getcwd()
    embeddings_cache_file = current_dir + "/embeddings_cache_file_word2vec.pkl.bz2"

    # load pre-extracted word-to-index maps and pre-filtered Glove embeddings
    word_to_indices_map, word_index_to_embeddings_map = \
        vocabulary_embeddings_extractor.load_cached_vocabulary_and_embeddings(embeddings_cache_file)

    (train_instance_id_list, train_warrant0_list, train_warrant1_list, train_correct_label_w0_or_w1_list,
     train_reason_list, train_claim_list, train_debate_meta_data_list) = \
        data_loader.load_single_file(current_dir + '/data/train-w-swap.tsv', word_to_indices_map)

    (dev_instance_id_list, dev_warrant0_list, dev_warrant1_list, dev_correct_label_w0_or_w1_list,
     dev_reason_list, dev_claim_list, dev_debate_meta_data_list) = \
        data_loader.load_single_file(current_dir + '/data/dev.tsv', word_to_indices_map)

    (test_instance_id_list, test_warrant0_list, test_warrant1_list, test_correct_label_w0_or_w1_list,
     test_reason_list, test_claim_list, test_debate_meta_data_list) = \
        data_loader.load_single_file(current_dir + '/data/test.tsv', word_to_indices_map)

    # pad all sequences
    (train_warrant0_list, train_warrant1_list, train_reason_list, train_claim_list, train_debate_meta_data_list) = [
        sequence.pad_sequences(x, maxlen=max_len) for x in
        (train_warrant0_list, train_warrant1_list, train_reason_list, train_claim_list, train_debate_meta_data_list)]
    (test_warrant0_list, test_warrant1_list, test_reason_list, test_claim_list, test_debate_meta_data_list) = [
        sequence.pad_sequences(x, maxlen=max_len) for x in
        (test_warrant0_list, test_warrant1_list, test_reason_list, test_claim_list, test_debate_meta_data_list)]

    (dev_warrant0_list, dev_warrant1_list, dev_reason_list, dev_claim_list, dev_debate_meta_data_list) = [
        sequence.pad_sequences(x, maxlen=max_len) for x in (dev_warrant0_list, dev_warrant1_list, dev_reason_list,
                                                            dev_claim_list, dev_debate_meta_data_list)]
    assert train_warrant0_list.shape == train_warrant1_list.shape == train_reason_list.shape == train_claim_list.shape == train_debate_meta_data_list.shape

    # ---------------
    all_runs_report = []  # list of dict

    # 3 repeats to show how much randomness is in it
    for i in range(1, 4):
        print("Run: ", i)

        np.random.seed(12345 + i)  # for reproducibility

        # simple bidi-lstm model
        # model = get_attention_lstm(word_index_to_embeddings_map, max_len, rich_context=False, dropout=dropout, lstm_size=lstm_size)
        # simple bidi-lstm model w/ context
        # model = get_attention_lstm(word_index_to_embeddings_map, max_len, rich_context=True, dropout=dropout, lstm_size=lstm_size)
        # intra-warrant attention
        # model = get_attention_lstm_intra_warrant(word_index_to_embeddings_map, max_len, rich_context=False, dropout=dropout, lstm_size=lstm_size)
        # intra-warrant w/ context
        model = get_attention_lstm_intra_warrant(word_index_to_embeddings_map, max_len, rich_context=True, dropout=dropout, lstm_size=lstm_size)

        model.fit(
            {'sequence_layer_warrant0_input': train_warrant0_list, 'sequence_layer_warrant1_input': train_warrant1_list,
             'sequence_layer_reason_input': train_reason_list, 'sequence_layer_claim_input': train_claim_list,
             'sequence_layer_debate_input': train_debate_meta_data_list},
            train_correct_label_w0_or_w1_list, nb_epoch=nb_epoch, batch_size=batch_size, verbose=verbose,
            validation_split=0.1)

        # model predictions
        predicted_probabilities_dev = model.predict(
            {'sequence_layer_warrant0_input': dev_warrant0_list, 'sequence_layer_warrant1_input': dev_warrant1_list,
             'sequence_layer_reason_input': dev_reason_list, 'sequence_layer_claim_input': dev_claim_list,
             'sequence_layer_debate_input': dev_debate_meta_data_list},
            batch_size=batch_size, verbose=1)

        predicted_probabilities_test = model.predict(
            {'sequence_layer_warrant0_input': test_warrant0_list, 'sequence_layer_warrant1_input': test_warrant1_list,
             'sequence_layer_reason_input': test_reason_list, 'sequence_layer_claim_input': test_claim_list,
             'sequence_layer_debate_input': test_debate_meta_data_list},
            batch_size=batch_size, verbose=1)

        predicted_labels_dev = get_predicted_labels(predicted_probabilities_dev)
        predicted_labels_test = get_predicted_labels(predicted_probabilities_test)

        assert isinstance(test_correct_label_w0_or_w1_list, list)
        assert isinstance(test_correct_label_w0_or_w1_list[0], int)
        assert len(test_correct_label_w0_or_w1_list) == len(predicted_labels_test)
        acc_dev = accuracy(dev_correct_label_w0_or_w1_list, predicted_labels_dev)
        acc_test = accuracy(test_correct_label_w0_or_w1_list, predicted_labels_test)
        print('Dev accuracy:', acc_dev)
        print('Test accuracy:', acc_test)
        # update report
        report = dict()
        report['acc_dev'] = acc_dev
        report['acc_test'] = acc_test
        report['gold_labels_dev'] = dev_correct_label_w0_or_w1_list
        report['gold_labels_test'] = test_correct_label_w0_or_w1_list
        report['predicted_labels_dev'] = predicted_labels_dev
        report['predicted_labels_test'] = predicted_labels_test
        report['ids_test'] = test_instance_id_list
        report['ids_dev'] = dev_instance_id_list
        all_runs_report.append(report)
        # report_description = description + str(args).replace("\n", " ")
        # finish_report(report, report_description, output_file)

    # show report
    print("Acc dev")
    for r in all_runs_report:
        print("%.3f\t" % r['acc_dev'], end='')
    print("\nAcc test")
    for r in all_runs_report:
        print("%.3f\t" % r['acc_test'], end='')
    print("\nInstances correct")
    for r in all_runs_report:
        good_ids = set()
        wrong_ids = set()
        for i, (g, p, instance_id) in enumerate(zip(r['gold_labels_dev'], r['predicted_labels_dev'], r['ids_dev'])):
            if g == p:
                good_ids.add(instance_id)
            else:
                wrong_ids.add(instance_id)
        print("Good_ids\t", good_ids)
        print("Wrong_ids\t", wrong_ids)


def print_error_analysis_dev(ids: set) -> None:
    """
    Prints instances given in the ids parameter; reads data from dev.tsv
    :param ids: ids
    :return: none
    """
    f = open('data/dev.tsv', 'r')
    lines = f.readlines()
    # remove first line with comments
    del lines[0]

    for line in lines:
        split_line = line.split('\t')
        # "#id warrant0 warrant1 correctLabelW0orW1 reason claim debateTitle debateInfo
        assert len(split_line) == 8

        instance_id = split_line[0]

        if instance_id in ids:
            print(line.strip())


if __name__ == "__main__":
    __main__()
