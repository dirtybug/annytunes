import 'package:flutter/material.dart';
import 'package:annytunes/core/models/models.dart';
import 'frequency_input_field.dart';

/// A modal bottom sheet for editing a single channel's settings.
class ChannelEditSheet extends StatefulWidget {
  final Channel channel;
  final int channelNumber;
  final ValueChanged<Channel> onSave;

  const ChannelEditSheet({
    super.key,
    required this.channel,
    required this.channelNumber,
    required this.onSave,
  });

  /// Shows the edit sheet and returns the edited channel, or null if cancelled.
  static Future<Channel?> show(
    BuildContext context,
    Channel channel,
    int number,
  ) {
    return showModalBottomSheet<Channel>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (context) => ChannelEditSheet(
        channel: channel,
        channelNumber: number,
        onSave: (edited) => Navigator.of(context).pop(edited),
      ),
    );
  }

  @override
  State<ChannelEditSheet> createState() => _ChannelEditSheetState();
}

class _ChannelEditSheetState extends State<ChannelEditSheet> {
  final _formKey = GlobalKey<FormState>();

  late String _name;
  late int _rxHz;
  late int _txHz;
  late bool _digital;
  late int _colorCode;
  late int _timeslot;
  late int _contactId;
  late AdmitCriteria _admit;
  late PowerLevel _power;
  late double _bandwidthKHz;

  @override
  void initState() {
    super.initState();
    final ch = widget.channel;
    _name = ch.name;
    _rxHz = ch.rxHz;
    _txHz = ch.txHz;
    _digital = ch.digital;
    _colorCode = ch.colorCode;
    _timeslot = ch.timeslot;
    _contactId = ch.contactId;
    _admit = ch.admit;
    _power = ch.power;
    _bandwidthKHz = ch.bandwidthKHz;
  }

  void _save() {
    if (!_formKey.currentState!.validate()) return;
    _formKey.currentState!.save();

    widget.onSave(widget.channel.copyWith(
      name: _name,
      rxHz: _rxHz,
      txHz: _txHz,
      digital: _digital,
      colorCode: _colorCode,
      timeslot: _timeslot,
      contactId: _contactId,
      admit: _admit,
      power: _power,
      bandwidthKHz: _bandwidthKHz,
      edited: true,
    ));
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.85,
      minChildSize: 0.5,
      maxChildSize: 0.95,
      expand: false,
      builder: (context, scrollController) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: Column(
          children: [
            _buildDragHandle(),
            _buildTitle(),
            const Divider(height: 1),
            Expanded(
              child: Form(
                key: _formKey,
                child: ListView(
                  controller: scrollController,
                  padding: const EdgeInsets.all(16),
                  children: [
                    _buildNameField(),
                    const SizedBox(height: 16),
                    FrequencyInputField(
                      label: 'RX Frequency',
                      valueHz: _rxHz,
                      onChanged: (hz) => setState(() => _rxHz = hz),
                    ),
                    const SizedBox(height: 16),
                    FrequencyInputField(
                      label: 'TX Frequency',
                      valueHz: _txHz,
                      onChanged: (hz) => setState(() => _txHz = hz),
                    ),
                    const SizedBox(height: 16),
                    _buildModeSelector(),
                    _buildDmrFields(),
                    const SizedBox(height: 16),
                    _buildAdmitDropdown(),
                    const SizedBox(height: 16),
                    _buildPowerDropdown(),
                    const SizedBox(height: 16),
                    _buildBandwidthSelector(),
                  ],
                ),
              ),
            ),
            _buildActions(),
          ],
        ),
      ),
    );
  }

  Widget _buildDragHandle() {
    return Center(
      child: Container(
        margin: const EdgeInsets.only(top: 12, bottom: 4),
        width: 32,
        height: 4,
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.onSurfaceVariant.withValues(alpha: 0.4),
          borderRadius: BorderRadius.circular(2),
        ),
      ),
    );
  }

  Widget _buildTitle() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Text(
        'Edit Channel #${widget.channelNumber}',
        style: Theme.of(context).textTheme.titleLarge,
      ),
    );
  }

  Widget _buildNameField() {
    return TextFormField(
      initialValue: _name,
      decoration: const InputDecoration(
        labelText: 'Channel Name',
        border: OutlineInputBorder(),
      ),
      maxLength: 16,
      validator: (v) => (v == null || v.trim().isEmpty) ? 'Required' : null,
      onChanged: (v) => _name = v,
    );
  }

  Widget _buildModeSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Mode', style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 8),
        SegmentedButton<bool>(
          segments: const [
            ButtonSegment(value: false, label: Text('Analog')),
            ButtonSegment(value: true, label: Text('DMR')),
          ],
          selected: {_digital},
          onSelectionChanged: (v) => setState(() => _digital = v.first),
        ),
      ],
    );
  }

  Widget _buildDmrFields() {
    return AnimatedSize(
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeInOut,
      alignment: Alignment.topCenter,
      child: _digital
          ? Padding(
              padding: const EdgeInsets.only(top: 16),
              child: Column(
                children: [
                  DropdownButtonFormField<int>(
                    value: _colorCode,
                    decoration: const InputDecoration(
                      labelText: 'Color Code',
                      border: OutlineInputBorder(),
                    ),
                    items: List.generate(
                      16,
                      (i) => DropdownMenuItem(value: i, child: Text('$i')),
                    ),
                    onChanged: (v) {
                      if (v != null) setState(() => _colorCode = v);
                    },
                  ),
                  const SizedBox(height: 16),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Timeslot',
                          style: Theme.of(context).textTheme.bodySmall),
                      const SizedBox(height: 8),
                      SegmentedButton<int>(
                        segments: const [
                          ButtonSegment(value: 1, label: Text('TS1')),
                          ButtonSegment(value: 2, label: Text('TS2')),
                        ],
                        selected: {_timeslot},
                        onSelectionChanged: (v) =>
                            setState(() => _timeslot = v.first),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    initialValue: _contactId > 0 ? '$_contactId' : '',
                    decoration: const InputDecoration(
                      labelText: 'Contact ID',
                      border: OutlineInputBorder(),
                    ),
                    keyboardType: TextInputType.number,
                    onChanged: (v) =>
                        _contactId = int.tryParse(v) ?? _contactId,
                  ),
                ],
              ),
            )
          : const SizedBox.shrink(),
    );
  }

  Widget _buildAdmitDropdown() {
    return DropdownButtonFormField<AdmitCriteria>(
      value: _admit,
      decoration: const InputDecoration(
        labelText: 'Admit',
        border: OutlineInputBorder(),
      ),
      items: AdmitCriteria.values
          .map((a) => DropdownMenuItem(value: a, child: Text(a.label)))
          .toList(),
      onChanged: (v) {
        if (v != null) setState(() => _admit = v);
      },
    );
  }

  Widget _buildPowerDropdown() {
    return DropdownButtonFormField<PowerLevel>(
      value: _power,
      decoration: const InputDecoration(
        labelText: 'Power',
        border: OutlineInputBorder(),
      ),
      items: PowerLevel.values
          .map((p) => DropdownMenuItem(value: p, child: Text(p.label)))
          .toList(),
      onChanged: (v) {
        if (v != null) setState(() => _power = v);
      },
    );
  }

  Widget _buildBandwidthSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Bandwidth', style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 8),
        SegmentedButton<double>(
          segments: const [
            ButtonSegment(value: 12.5, label: Text('12.5 kHz')),
            ButtonSegment(value: 25.0, label: Text('25 kHz')),
          ],
          selected: {_bandwidthKHz},
          onSelectionChanged: (v) =>
              setState(() => _bandwidthKHz = v.first),
        ),
      ],
    );
  }

  Widget _buildActions() {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          const SizedBox(width: 8),
          FilledButton(
            onPressed: _save,
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }
}
